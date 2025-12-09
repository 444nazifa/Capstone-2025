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
You are CareCapsule, a helpful app assistant.

Your responsibilities:
- Summarize the user’s medication reminders based ONLY on reminder data passed in the request.
- Help the user navigate the app (how to scan pill bottles, edit reminders, view info, etc.).
- Explain how to use app features in simple language.

STRICT SAFETY RULES:
- Do NOT give medical advice or dosing instructions.
- Do NOT recommend taking more or less medication under any circumstances.
- Do NOT interpret symptoms or emergencies.
- If asked medical questions, ALWAYS respond with:
  "I’m really sorry, but I can’t give medical advice. Please contact your doctor."

Stay concise and friendly.
`;

// POST /chat
// body: { message: string, reminders?: any }
router.post("/", async (req: Request, res: Response) => {
  const { message, reminders } = req.body as {
    message?: string;
    reminders?: unknown;
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

  // Hard safety guard: never let medication-change questions reach the model
  const lower = message.toLowerCase();
  const bannedPatterns = [
    "take an extra",
    "take more",
    "double my dose",
    "increase my dose",
    "lower my dose",
    "is it safe to take",
    "my blood pressure is high",
    "my sugar is high",
    "should i stop taking"
  ];
  if (bannedPatterns.some((p) => lower.includes(p))) {
    return res.json({
      reply:
        "I’m really sorry, but I can’t give medical advice or recommend medication changes. Please contact your doctor!",
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
      )}\n\nUse this ONLY to summarize or explain schedules.`;
    }

    const fullPrompt = `
${SYSTEM_PROMPT}

User message:
"${message}"

${remindersContext}
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
