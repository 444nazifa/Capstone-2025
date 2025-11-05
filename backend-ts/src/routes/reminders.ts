import { Router, Response } from 'express';
import { supabaseAdmin } from '../config/supabase';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';
import { UserMedicationResponse } from '../types/medication';

/**
 * Reminders API Routes
 *
 * This provides a simplified interface for managing medication reminders.
 * Built on top of the user_medications and medication_schedules tables.
 *
 * Original concept by: Mohamed Ali
 * Refactored to use medication infrastructure with authentication and push notifications
 */

const router = Router();

/**
 * @route POST /api/reminders
 * @desc Create a new medication reminder
 * @body { medication_name, dosage, scheduled_time, days_of_week, instructions }
 * @access Private (requires authentication)
 */
router.post('/', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { medication_name, dosage, scheduled_time, days_of_week, instructions, description } = req.body;

    if (!medication_name || !dosage || !scheduled_time) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields (medication_name, dosage, scheduled_time)'
      });
    }

    // Create the medication entry
    const { data: medication, error: medicationError } = await supabaseAdmin
      .from('user_medications')
      .insert({
        user_id: req.user.id,
        medication_name,
        dosage,
        instructions: instructions || description,
        frequency: 'Custom schedule',
        is_active: true
      })
      .select()
      .single();

    if (medicationError || !medication) {
      console.error('Medication creation error:', medicationError);
      return res.status(500).json({
        success: false,
        message: 'Failed to create reminder'
      });
    }

    // Create the schedule
    const { data: schedule, error: scheduleError } = await supabaseAdmin
      .from('medication_schedules')
      .insert({
        user_medication_id: medication.id,
        scheduled_time,
        days_of_week: days_of_week || [0, 1, 2, 3, 4, 5, 6], // Default to every day
        is_enabled: true
      })
      .select()
      .single();

    if (scheduleError) {
      console.error('Schedule creation error:', scheduleError);
    } else {
      medication.schedules = [schedule];
    }

    return res.status(201).json({
      success: true,
      message: 'Reminder created successfully',
      medication
    });

  } catch (err) {
    console.error('Create reminder error:', err);
    return res.status(500).json({
      success: false,
      message: err instanceof Error ? err.message : 'Internal server error'
    });
  }
});

/**
 * @route GET /api/reminders
 * @desc Get all reminders for the authenticated user
 * @access Private
 */
router.get('/', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { data: medications, error } = await supabaseAdmin
      .from('user_medications')
      .select('*, schedules:medication_schedules(*)')
      .eq('user_id', req.user.id)
      .eq('is_active', true)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Get reminders error:', error);
      return res.status(500).json({
        success: false,
        message: 'Failed to fetch reminders'
      });
    }

    return res.json({
      success: true,
      message: 'Reminders retrieved successfully',
      medications: medications || []
    });

  } catch (err) {
    console.error('Get reminders error:', err);
    return res.status(500).json({
      success: false,
      message: err instanceof Error ? err.message : 'Internal server error'
    });
  }
});

/**
 * @route PUT /api/reminders/:id
 * @desc Update a reminder (medication and/or schedule)
 * @access Private
 */
router.put('/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;
    const { medication_name, dosage, instructions, scheduled_time, days_of_week, is_active } = req.body;

    // Verify medication belongs to user
    const { data: existingMed, error: checkError } = await supabaseAdmin
      .from('user_medications')
      .select('id')
      .eq('id', id)
      .eq('user_id', req.user.id)
      .single();

    if (checkError || !existingMed) {
      return res.status(404).json({
        success: false,
        message: 'Reminder not found'
      });
    }

    // Update medication fields if provided
    const medicationUpdates: any = {};
    if (medication_name !== undefined) medicationUpdates.medication_name = medication_name;
    if (dosage !== undefined) medicationUpdates.dosage = dosage;
    if (instructions !== undefined) medicationUpdates.instructions = instructions;
    if (is_active !== undefined) medicationUpdates.is_active = is_active;

    if (Object.keys(medicationUpdates).length > 0) {
      const { error: updateError } = await supabaseAdmin
        .from('user_medications')
        .update(medicationUpdates)
        .eq('id', id);

      if (updateError) {
        console.error('Medication update error:', updateError);
        return res.status(500).json({
          success: false,
          message: 'Failed to update reminder'
        });
      }
    }

    // Update schedule if time or days changed
    if (scheduled_time !== undefined || days_of_week !== undefined) {
      const scheduleUpdates: any = {};
      if (scheduled_time !== undefined) scheduleUpdates.scheduled_time = scheduled_time;
      if (days_of_week !== undefined) scheduleUpdates.days_of_week = days_of_week;

      const { error: scheduleError } = await supabaseAdmin
        .from('medication_schedules')
        .update(scheduleUpdates)
        .eq('user_medication_id', id);

      if (scheduleError) {
        console.error('Schedule update error:', scheduleError);
      }
    }

    // Fetch updated medication with schedules
    const { data: medication, error: fetchError } = await supabaseAdmin
      .from('user_medications')
      .select('*, schedules:medication_schedules(*)')
      .eq('id', id)
      .single();

    if (fetchError || !medication) {
      return res.status(500).json({
        success: false,
        message: 'Failed to fetch updated reminder'
      });
    }

    return res.json({
      success: true,
      message: 'Reminder updated successfully',
      medication
    });

  } catch (err) {
    console.error('Update reminder error:', err);
    return res.status(500).json({
      success: false,
      message: err instanceof Error ? err.message : 'Internal server error'
    });
  }
});

/**
 * @route DELETE /api/reminders/:id
 * @desc Delete a reminder (soft delete by marking inactive)
 * @access Private
 */
router.delete('/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;

    // Soft delete by marking as inactive
    const { error } = await supabaseAdmin
      .from('user_medications')
      .update({ is_active: false })
      .eq('id', id)
      .eq('user_id', req.user.id);

    if (error) {
      console.error('Delete reminder error:', error);
      return res.status(404).json({
        success: false,
        message: 'Reminder not found or delete failed'
      });
    }

    return res.json({
      success: true,
      message: 'Reminder deleted successfully'
    });

  } catch (err) {
    console.error('Delete reminder error:', err);
    return res.status(500).json({
      success: false,
      message: err instanceof Error ? err.message : 'Internal server error'
    });
  }
});

/**
 * @route POST /api/reminders/:id/complete
 * @desc Mark a reminder as completed (taken)
 * @access Private
 */
router.post('/:id/complete', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;
    const { notes } = req.body;

    // Verify medication belongs to user
    const { data: medication, error: medError } = await supabaseAdmin
      .from('user_medications')
      .select('id')
      .eq('id', id)
      .eq('user_id', req.user.id)
      .single();

    if (medError || !medication) {
      return res.status(404).json({
        success: false,
        message: 'Reminder not found'
      });
    }

    // Create history entry marking it as taken
    const { data: history, error: historyError } = await supabaseAdmin
      .from('medication_history')
      .insert({
        user_id: req.user.id,
        user_medication_id: id,
        scheduled_at: new Date().toISOString(),
        taken_at: new Date().toISOString(),
        status: 'taken',
        notes
      })
      .select()
      .single();

    if (historyError || !history) {
      return res.status(500).json({
        success: false,
        message: 'Failed to mark reminder as completed'
      });
    }

    return res.status(201).json({
      success: true,
      message: 'Reminder marked as completed'
    });

  } catch (err) {
    console.error('Complete reminder error:', err);
    return res.status(500).json({
      success: false,
      message: err instanceof Error ? err.message : 'Internal server error'
    });
  }
});

export default router;
