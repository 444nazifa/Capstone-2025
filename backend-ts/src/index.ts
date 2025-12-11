import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import authRoutes from './routes/auth';
import medicationRoutes from './routes/medication';
import userMedicationsRoutes from './routes/userMedications';
import deviceTokenRoutes from './routes/device-token.routes';
import reminderRoutes from './routes/reminders';
import { errorHandler, notFoundHandler } from './middleware/errorHandler';
import firebaseService from './services/firebase.service';
import reminderScheduler from './services/reminder-scheduler.service';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3015;

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: {
    success: false,
    message: 'Too many requests from this IP, please try again later.'
  }
});

app.use(helmet());
app.use(cors());
app.use(limiter);
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));


app.get('/health', (req, res) => {
  res.json({
    success: true,
    message: 'Server is running',
    timestamp: new Date().toISOString()
  });
});

app.get('/api/cron/check-reminders', async (req, res) => {
  try {
    const cronSecret = req.query.secret || req.headers.authorization?.replace('Bearer ', '');

    if (cronSecret !== process.env.CRON_SECRET) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    await reminderScheduler.triggerCheck();
    return res.json({ success: true, message: 'Reminder check completed' });
  } catch (error) {
    console.error('Cron error:', error);
    return res.status(500).json({ error: 'Failed to check reminders' });
  }
});

app.use('/api/auth', authRoutes);
app.use('/api/medication', medicationRoutes);
app.use('/api/medications', userMedicationsRoutes);
app.use('/api/device-tokens', deviceTokenRoutes);
app.use('/api/reminders', reminderRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

// Initialize Firebase for push notifications
firebaseService.initializeFirebase();

// Only start scheduler if not on Vercel (for local development)
if (!process.env.VERCEL) {
  reminderScheduler.startScheduler();
}

// Cleanup on shutdown
process.on('SIGTERM', () => {
  reminderScheduler.stopScheduler();
  process.exit(0);
});