import { Router, Request, Response } from "express";
import { createClient } from "@supabase/supabase-js";
import dotenv from "dotenv";

dotenv.config();

const router = Router();

// 
const supabase = createClient
(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

/**
 * @route POST /api/reminders
 * @desc Crete a new reminder
 * @body { user_id, title, description, reminder_time }
 */
router.post("/", async (req: Request, res: Response) => 
    {
  try {
    const { user_id, title, description, reminder_time } = req.body;

    if (!user_id || !title || !reminder_time) 
    {
      return res.status(400).json({
        success: false,
        error: "Missing required fields (user_id, title, reminder_time)",
      });
    }

    const { data, error } = await supabase
      .from("reminders")
      .insert([
        {
          user_id,
          title,
          description,
          reminder_time,
        },
      ])
      .select("*");

    if (error) throw error;

    return res.status(201).json({
      success: true,
      data,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err instanceof Error ? err.message : "Internal server error",
    });
  }
});

/**
 * @route GET /api/reminders/:user_id
 * @desc Get all remindrs for a specific user
 */
router.get("/:user_id", async (req: Request, res: Response) => 
    {
  try {
    const { user_id } = req.params;

    const { data, error } = await supabase
      .from("reminders")
      .select("*")
      .eq("user_id", user_id)
      .order("reminder_time", { ascending: true });

    if (error) throw error;

    return res.json({
      success: true,
      data,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err instanceof Error ? err.message : "Internal server error",
    });
  }
});

/**
 * @route PUT /api/reminders/:id
 * @desc Update a reminder (e.g. mark complete or change time)
 */
router.put("/:id", async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { title, description, reminder_time, is_completed } = req.body;

    const updateFields: any = {};
    if (title !== undefined) updateFields.title = title;
    if (description !== undefined) updateFields.description = description;
    if (reminder_time !== undefined) updateFields.reminder_time = reminder_time;
    if (is_completed !== undefined) updateFields.is_completed = is_completed;

    const { data, error } = await supabase
      .from("reminders")
      .update(updateFields)
      .eq("id", id)
      .select("*");

    if (error) throw error;

    return res.json({
      success: true,
      data,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err instanceof Error ? err.message : "Internal server error",
    });
  }
});

/**
 * @route DELETE /api/reminders/:id
 * @desc Delete a reminder by ID
 */
router.delete("/:id", async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const { error } = await supabase.from("reminders").delete().eq("id", id);

    if (error) throw error;

    return res.json({
      success: true,
      message: "Reminder deleted successfully",
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err instanceof Error ? err.message : "Internal server error",
    });
  }
});

export default router;
