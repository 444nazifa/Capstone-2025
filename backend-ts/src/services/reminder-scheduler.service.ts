import { supabaseAdmin } from '../config/supabase';
import notificationService from './notification.service';

// NOTE: Scheduler is NOT automatically started. Call startScheduler() to enable.

class ReminderSchedulerService {
  private static instance: ReminderSchedulerService;
  private schedulerInterval: NodeJS.Timeout | null = null;
  private isRunning = false;
  private readonly CHECK_INTERVAL = 60 * 1000;

  private constructor() {}

  static getInstance(): ReminderSchedulerService {
    if (!ReminderSchedulerService.instance) {
      ReminderSchedulerService.instance = new ReminderSchedulerService();
    }
    return ReminderSchedulerService.instance;
  }

  startScheduler(): void {
    if (this.isRunning) {
      console.log('⚠️  Reminder scheduler is already running');
      return;
    }

    console.log('📅 Starting reminder scheduler...');
    this.isRunning = true;

    // Run immediately on start
    this.checkDueReminders();

    // Then run at regular intervals
    this.schedulerInterval = setInterval(() => {
      this.checkDueReminders();
    }, this.CHECK_INTERVAL);

    console.log(`✅ Reminder scheduler started (checking every ${this.CHECK_INTERVAL / 1000}s)`);
  }

  stopScheduler(): void {
    if (!this.isRunning) {
      console.log('⚠️  Reminder scheduler is not running');
      return;
    }

    if (this.schedulerInterval) {
      clearInterval(this.schedulerInterval);
      this.schedulerInterval = null;
    }

    this.isRunning = false;
    console.log('✅ Reminder scheduler stopped');
  }

  private async checkDueReminders(): Promise<void> {
    try {
      const now = new Date();
      const currentTime = now.toTimeString().split(' ')[0]?.substring(0, 5);
      const currentDayOfWeek = now.getDay();

      const { data: schedules, error: schedulesError } = await supabaseAdmin
        .from('medication_schedules')
        .select(`
          id,
          user_medication_id,
          scheduled_time,
          days_of_week,
          is_enabled,
          user_medications!inner (
            id,
            user_id,
            medication_name,
            dosage,
            is_active
          )
        `)
        .eq('is_enabled', true)
        .eq('user_medications.is_active', true);

      if (schedulesError) {
        console.error('Error fetching schedules:', schedulesError);
        return;
      }

      if (!schedules || schedules.length === 0) {
        console.log('✅ No schedules found');
        return;
      }

      const remindersToSend = [];
      const today = now.toISOString().split('T')[0];
      for (const schedule of schedules) {
        const scheduledTime = schedule.scheduled_time.substring(0, 5);

        if (!schedule.days_of_week.includes(currentDayOfWeek)) {
          continue;
        }

        if (scheduledTime !== currentTime) {
          continue;
        }

        const medication = Array.isArray(schedule.user_medications)
          ? schedule.user_medications[0]
          : schedule.user_medications;

        if (!medication) continue;

        const scheduledAt = `${today}T${schedule.scheduled_time}`;

        const { data: history, error: historyError } = await supabaseAdmin
          .from('medication_history')
          .select('id')
          .eq('user_medication_id', schedule.user_medication_id)
          .eq('medication_schedule_id', schedule.id)
          .eq('scheduled_at', scheduledAt) 
          .in('status', ['taken', 'skipped'])
          .limit(1);

        if (historyError) {
          console.error('Error checking history:', historyError);
          continue;
        }

        if (!history || history.length === 0) {
          remindersToSend.push({ schedule, medication, scheduledAt });
        }
      }

      if (remindersToSend.length === 0) {
        console.log('✅ No new reminders to send at', now.toISOString());
        return;
      }

      console.log(`📬 Found ${remindersToSend.length} due reminder(s)`);

      for (const { schedule, medication, scheduledAt } of remindersToSend) {
        try {
          const success = await notificationService.sendMedicationReminder(
            medication.user_id, 
            {
              id: medication.id,
              name: medication.medication_name,
              dosage: medication.dosage
            },
            schedule.id 
          );

          if (success) {
            console.log(`✅ Reminder sent for medication: ${medication.medication_name}`);
          } else {
            console.log(`⚠️  No devices to notify for user: ${medication.user_id}`);
          }
        } catch (error) {
          console.error(`❌ Error sending reminder:`, error);
        }
      }
    } catch (error) {
      console.error('❌ Error checking due reminders:', error);
    }
  }

  async triggerCheck(): Promise<void> {
    console.log('🔄 Manually triggering reminder check...');
    await this.checkDueReminders();
  }

  isSchedulerRunning(): boolean {
    return this.isRunning;
  }
}

export default ReminderSchedulerService.getInstance();
