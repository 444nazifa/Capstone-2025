import express, { Request, Response } from "express";
import axios from "axios";

const router = express.Router();

const apiKey = process.env.GEMINI_API_KEY;
if (!apiKey) {
  console.warn("⚠️ GEMINI_API_KEY is not set. /chat will fail for normal questions.");
}

// Use gemini-2.0-flash on the v1beta generateContent endpoint
const GEMINI_ENDPOINT = 
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

const SYSTEM_PROMPT = `
You are CareCapsule, a helpful and knowledgeable app assistant with access to the user's medication list.

Your responsibilities:
- Summarize the user's medication reminders and current medications based on data passed in the request.
- Answer questions about drug interactions between medications the user is taking.
- Provide general information about medications (purpose, common side effects, storage).
- Explain potential interactions when asked about taking new medications with their current list.
- Help the user navigate the app (how to scan pill bottles, edit reminders, view info, etc.).
- Explain how to use app features in simple language.

MEDICATION INTERACTION GUIDELINES:
- You CAN discuss general drug interactions between medications based on established pharmacological knowledge.
- You CAN explain common side effects and what medications are typically used for.
- When discussing interactions, ALWAYS include this disclaimer: "This is general information. Always consult your doctor or pharmacist before making any changes to your medications."
- Be clear when interactions are serious vs. minor.

STRICT SAFETY RULES:
- Do NOT recommend changing dosages or stopping medications.
- Do NOT recommend taking more or less medication under any circumstances.
- Do NOT diagnose conditions or interpret symptoms.
- Do NOT prescribe or recommend starting new medications.
- If asked about serious symptoms or emergencies, ALWAYS respond with:
  "This sounds serious. Please contact your doctor immediately or call emergency services."
- For dosage questions, respond with:
  "I can't advise on dosage changes. Please consult your doctor or pharmacist."

Stay concise, friendly, and always prioritize user safety.
`;

// POST /chat
// body: { message: string, reminders?: any, medications?: any }
router.post("/", async (req: Request, res: Response) => {
  const { message, reminders, medications } = req.body as {
    message?: string;
    reminders?: unknown;
    medications?: unknown;
  };

  if (!message || typeof message !== "string") {
    return res.status(400).json({ error: "Missing 'message' in request body" });
  }

  // If no API key, fail fast (so you KNOW it's not using AI)
  if (!apiKey) {
    return res
      .status(500)
      .json({ error: "GEMINI_API_KEY is not set on the server", source: "none" });
  }

  // Hard safety guard: block dosage and emergency questions
  const lower = message.toLowerCase();
  const bannedPatterns = [
    "take an extra",
    "take more",
    "double my dose",
    "increase my dose",
    "lower my dose",
    "reduce my dose",
    "should i stop taking",
    "can i stop taking"
  ];
  if (bannedPatterns.some((p) => lower.includes(p))) {
    return res.json({
      reply:
        "I can't advise on dosage changes or stopping medications. Please consult your doctor or pharmacist.",
      source: "rule"
    });
  }

  try {
    let remindersContext = "";
    if (reminders) {
      remindersContext = `Here is the user's reminder data (JSON):\n${JSON.stringify(
        reminders,
        null,
        2
      )}\n\nUse this to summarize or explain schedules.`;
    }

    let medicationsContext = "";
    if (medications) {
      medicationsContext = `Here is the user's current medication list (JSON):\n${JSON.stringify(
        medications,
        null,
        2
      )}\n\nUse this to answer questions about their medications, potential interactions, and general information. Always include appropriate disclaimers.`;
    }

    const fullPrompt = `
${SYSTEM_PROMPT}

User message:
"${message}"

${remindersContext}

${medicationsContext}
`;

    const body = {
      contents: [
        {
          role: "user",
          parts: [{ text: fullPrompt }]
        }
      ]
    };

    const response = await axios.post(
      `${GEMINI_ENDPOINT}?key=${apiKey}`,
      body,
      {
        headers: { "Content-Type": "application/json" },
        timeout: 8000
      }
    );

    const data = response.data;

    if (!data?.candidates?.length) {
      console.error("🔥 gemini-2.0-flash returned no candidates:", data);
      return res
        .status(502)
        .json({ error: "Gemini returned no candidates", source: "ai" });
    }

    const parts = data.candidates[0].content?.parts || [];
    const replyText = parts.map((p: any) => p.text || "").join("");

    return res.json({ reply: replyText, source: "ai" });
  } catch (err: any) {
    console.error(
      "🔥 Error calling gemini-2.0-flash:",
      err?.response?.data || err?.message || err
    );
    return res.status(502).json({
      error: "Failed to generate response from Gemini",
      details: err?.response?.data || err?.message,
      source: "ai"
    });
  }
});

export default router;
