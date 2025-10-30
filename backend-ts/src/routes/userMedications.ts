import { Router, Request, Response } from 'express';
import { supabaseAdmin } from '../config/supabase';
import {
  createUserMedicationSchema,
  updateUserMedicationSchema,
  createScheduleSchema,
  updateScheduleSchema,
  markMedicationSchema
} from '../utils/validation';
import {
  CreateUserMedicationRequest,
  UpdateUserMedicationRequest,
  CreateScheduleRequest,
  UpdateScheduleRequest,
  MarkMedicationRequest,
  UserMedicationResponse,
  MedicationHistoryResponse
} from '../types/medication';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';

const router = Router();

router.post('/user', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { error, value } = createUserMedicationSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: error.details.map(detail => detail.message).join(', ')
      });
    }

    const { schedules, ...medicationData } = value as CreateUserMedicationRequest;

    // Insert medication
    const { data: medication, error: medicationError } = await supabaseAdmin
      .from('user_medications')
      .insert({
        ...medicationData,
        user_id: req.user.id
      })
      .select()
      .single();

    if (medicationError || !medication) {
      console.error('Medication creation error:', medicationError);
      return res.status(500).json({
        success: false,
        message: 'Failed to create medication'
      });
    }

    // Insert schedules if provided
    if (schedules && schedules.length > 0) {
      const schedulesToInsert = schedules.map(schedule => ({
        ...schedule,
        user_medication_id: medication.id
      }));

      const { data: createdSchedules, error: schedulesError } = await supabaseAdmin
        .from('medication_schedules')
        .insert(schedulesToInsert)
        .select();

      if (schedulesError) {
        console.error('Schedules creation error:', schedulesError);
      } else {
        medication.schedules = createdSchedules;
      }
    }

    return res.status(201).json({
      success: true,
      message: 'Medication created successfully',
      medication
    });

  } catch (error) {
    console.error('Create medication error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.get('/user', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { active } = req.query;
    let query = supabaseAdmin
      .from('user_medications')
      .select('*, schedules:medication_schedules(*)')
      .eq('user_id', req.user.id)
      .order('created_at', { ascending: false });

    if (active === 'true') {
      query = query.eq('is_active', true);
    }

    const { data: medications, error } = await query;

    if (error) {
      console.error('Get medications error:', error);
      return res.status(500).json({
        success: false,
        message: 'Failed to fetch medications'
      });
    }

    return res.json({
      success: true,
      message: 'Medications retrieved successfully',
      medications: medications || []
    });

  } catch (error) {
    console.error('Get medications error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.get('/user/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;

    const { data: medication, error } = await supabaseAdmin
      .from('user_medications')
      .select('*, schedules:medication_schedules(*)')
      .eq('id', id)
      .eq('user_id', req.user.id)
      .single();

    if (error || !medication) {
      return res.status(404).json({
        success: false,
        message: 'Medication not found'
      });
    }

    return res.json({
      success: true,
      message: 'Medication retrieved successfully',
      medication
    });

  } catch (error) {
    console.error('Get medication error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.put('/user/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;
    const { error, value } = updateUserMedicationSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: error.details.map(detail => detail.message).join(', ')
      });
    }

    const { data: medication, error: updateError } = await supabaseAdmin
      .from('user_medications')
      .update(value as UpdateUserMedicationRequest)
      .eq('id', id)
      .eq('user_id', req.user.id)
      .select('*, schedules:medication_schedules(*)')
      .single();

    if (updateError || !medication) {
      return res.status(404).json({
        success: false,
        message: 'Medication not found or update failed'
      });
    }

    return res.json({
      success: true,
      message: 'Medication updated successfully',
      medication
    });

  } catch (error) {
    console.error('Update medication error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.delete('/user/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;

    const { error } = await supabaseAdmin
      .from('user_medications')
      .delete()
      .eq('id', id)
      .eq('user_id', req.user.id);

    if (error) {
      return res.status(404).json({
        success: false,
        message: 'Medication not found or delete failed'
      });
    }

    return res.json({
      success: true,
      message: 'Medication deleted successfully'
    });

  } catch (error) {
    console.error('Delete medication error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.get('/summary', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    // Get all active medications
    const { data: medications, error: medsError } = await supabaseAdmin
      .from('user_medications')
      .select('*')
      .eq('user_id', req.user.id)
      .eq('is_active', true);

    if (medsError) {
      throw medsError;
    }

    const totalActive = medications?.length || 0;

    // Count low supply medications (< 25%)
    const lowSupplyCount = medications?.filter(med =>
      med.supply_remaining_percentage && med.supply_remaining_percentage < 25
    ).length || 0;

    // Count upcoming refills (within 7 days)
    const today = new Date();
    const weekFromNow = new Date(today);
    weekFromNow.setDate(weekFromNow.getDate() + 7);

    const upcomingRefills = medications?.filter(med => {
      if (!med.next_refill_date) return false;
      const refillDate = new Date(med.next_refill_date);
      return refillDate >= today && refillDate <= weekFromNow;
    }).length || 0;

    // Get today's schedule count
    const { data: todayHistory, error: historyError } = await supabaseAdmin
      .from('medication_history')
      .select('status')
      .eq('user_id', req.user.id)
      .gte('scheduled_at', new Date(today.setHours(0, 0, 0, 0)).toISOString())
      .lte('scheduled_at', new Date(today.setHours(23, 59, 59, 999)).toISOString());

    const medicationsDueToday = todayHistory?.length || 0;
    const takenToday = todayHistory?.filter(h => h.status === 'taken').length || 0;
    const adherenceRate = medicationsDueToday > 0
      ? Math.round((takenToday / medicationsDueToday) * 100)
      : undefined;

    return res.json({
      success: true,
      message: 'Summary retrieved successfully',
      summary: {
        total_active: totalActive,
        low_supply_count: lowSupplyCount,
        upcoming_refills: upcomingRefills,
        medications_due_today: medicationsDueToday,
        adherence_rate: adherenceRate
      }
    });

  } catch (error) {
    console.error('Get summary error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.post('/user/:id/schedules', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;
    const { error, value } = createScheduleSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: error.details.map(detail => detail.message).join(', ')
      });
    }

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
        message: 'Medication not found'
      });
    }

    const { data: schedule, error: scheduleError } = await supabaseAdmin
      .from('medication_schedules')
      .insert({
        ...value,
        user_medication_id: id
      })
      .select()
      .single();

    if (scheduleError || !schedule) {
      return res.status(500).json({
        success: false,
        message: 'Failed to create schedule'
      });
    }

    return res.status(201).json({
      success: true,
      message: 'Schedule created successfully'
    });

  } catch (error) {
    console.error('Create schedule error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.put('/schedules/:scheduleId', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { scheduleId } = req.params;
    const { error, value } = updateScheduleSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: error.details.map(detail => detail.message).join(', ')
      });
    }

    // Verify schedule belongs to user's medication
    const { data: schedule, error: scheduleError } = await supabaseAdmin
      .from('medication_schedules')
      .select('*, user_medications!inner(user_id)')
      .eq('id', scheduleId)
      .single();

    if (scheduleError || !schedule || schedule.user_medications?.user_id !== req.user.id) {
      return res.status(404).json({
        success: false,
        message: 'Schedule not found'
      });
    }

    const { data: updatedSchedule, error: updateError } = await supabaseAdmin
      .from('medication_schedules')
      .update(value as UpdateScheduleRequest)
      .eq('id', scheduleId)
      .select()
      .single();

    if (updateError || !updatedSchedule) {
      return res.status(500).json({
        success: false,
        message: 'Failed to update schedule'
      });
    }

    return res.json({
      success: true,
      message: 'Schedule updated successfully'
    });

  } catch (error) {
    console.error('Update schedule error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.delete('/schedules/:scheduleId', authenticateToken, async (req: AuthenticatedRequest, res: Response<UserMedicationResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { scheduleId } = req.params;

    // Verify schedule belongs to user's medication
    const { data: schedule, error: scheduleError } = await supabaseAdmin
      .from('medication_schedules')
      .select('*, user_medications!inner(user_id)')
      .eq('id', scheduleId)
      .single();

    if (scheduleError || !schedule || schedule.user_medications?.user_id !== req.user.id) {
      return res.status(404).json({
        success: false,
        message: 'Schedule not found'
      });
    }

    const { error } = await supabaseAdmin
      .from('medication_schedules')
      .delete()
      .eq('id', scheduleId);

    if (error) {
      return res.status(500).json({
        success: false,
        message: 'Failed to delete schedule'
      });
    }

    return res.json({
      success: true,
      message: 'Schedule deleted successfully'
    });

  } catch (error) {
    console.error('Delete schedule error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.post('/user/:id/mark', authenticateToken, async (req: AuthenticatedRequest, res: Response<MedicationHistoryResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { id } = req.params;
    const { error, value } = markMedicationSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: error.details.map(detail => detail.message).join(', ')
      });
    }

    const { status, taken_at, notes } = value as MarkMedicationRequest;

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
        message: 'Medication not found'
      });
    }

    const { data: history, error: historyError } = await supabaseAdmin
      .from('medication_history')
      .insert({
        user_id: req.user.id,
        user_medication_id: id,
        scheduled_at: new Date().toISOString(),
        taken_at: status === 'taken' ? (taken_at || new Date().toISOString()) : null,
        status,
        notes
      })
      .select()
      .single();

    if (historyError || !history) {
      return res.status(500).json({
        success: false,
        message: 'Failed to record medication history'
      });
    }

    return res.status(201).json({
      success: true,
      history: [history]
    });

  } catch (error) {
    console.error('Mark medication error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.get('/history', authenticateToken, async (req: AuthenticatedRequest, res: Response<MedicationHistoryResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { medication_id, start_date, end_date, status, limit = '50' } = req.query;

    let query = supabaseAdmin
      .from('medication_history')
      .select('*, user_medications(medication_name, dosage)')
      .eq('user_id', req.user.id)
      .order('scheduled_at', { ascending: false })
      .limit(parseInt(limit as string));

    if (medication_id) {
      query = query.eq('user_medication_id', medication_id);
    }

    if (start_date) {
      query = query.gte('scheduled_at', start_date);
    }

    if (end_date) {
      query = query.lte('scheduled_at', end_date);
    }

    if (status) {
      query = query.eq('status', status);
    }

    const { data: history, error, count } = await query;

    if (error) {
      console.error('Get history error:', error);
      return res.status(500).json({
        success: false,
        message: 'Failed to fetch medication history'
      });
    }

    return res.json({
      success: true,
      history: history || [],
      total: count || history?.length || 0
    });

  } catch (error) {
    console.error('Get history error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

export default router;
