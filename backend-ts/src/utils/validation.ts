import Joi from 'joi';

export const registerSchema = Joi.object({
  name: Joi.string().min(2).max(50).required().messages({
    'string.min': 'Name must be at least 2 characters long',
    'string.max': 'Name must not exceed 50 characters',
    'any.required': 'Name is required'
  }),
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address',
    'any.required': 'Email is required'
  }),
  password: Joi.string().min(8).pattern(new RegExp('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]')).required().messages({
    'string.min': 'Password must be at least 8 characters long',
    'string.pattern.base': 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character',
    'any.required': 'Password is required'
  }),
  date_of_birth: Joi.date().iso().max('now').required().messages({
    'date.base': 'Date of birth must be a valid date',
    'date.iso': 'Date of birth must be in ISO format (YYYY-MM-DD)',
    'date.max': 'Date of birth cannot be in the future',
    'any.required': 'Date of birth is required'
  })
});

export const loginSchema = Joi.object({
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address',
    'any.required': 'Email is required'
  }),
  password: Joi.string().required().messages({
    'any.required': 'Password is required'
  })
});

export const updateProfileSchema = Joi.object({
  name: Joi.string().min(2).max(50).optional().messages({
    'string.min': 'Name must be at least 2 characters long',
    'string.max': 'Name must not exceed 50 characters'
  }),
  email: Joi.string().email().optional().messages({
    'string.email': 'Please provide a valid email address'
  }),
  date_of_birth: Joi.date().iso().max('now').optional().messages({
    'date.base': 'Date of birth must be a valid date',
    'date.iso': 'Date of birth must be in ISO format (YYYY-MM-DD)',
    'date.max': 'Date of birth cannot be in the future'
  }),
  phone: Joi.string().pattern(/^\+?1?[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}$/).optional().messages({
    'string.pattern.base': 'Please provide a valid US phone number'
  })
});

const scheduleSchema = Joi.object({
  scheduled_time: Joi.string().pattern(/^([01]\d|2[0-3]):([0-5]\d)(:([0-5]\d))?$/).required().messages({
    'string.pattern.base': 'Scheduled time must be in HH:mm or HH:mm:ss format (e.g., "08:00", "14:30")',
    'any.required': 'Scheduled time is required'
  }),
  days_of_week: Joi.array().items(Joi.number().min(0).max(6)).min(1).max(7).default([0, 1, 2, 3, 4, 5, 6]).messages({
    'array.min': 'At least one day must be selected',
    'array.max': 'Days of week can only contain 0-6 (Sun-Sat)',
    'number.min': 'Day value must be between 0 (Sunday) and 6 (Saturday)',
    'number.max': 'Day value must be between 0 (Sunday) and 6 (Saturday)'
  }),
  is_enabled: Joi.boolean().default(true)
});

export const createUserMedicationSchema = Joi.object({
  medication_name: Joi.string().min(2).max(255).required().messages({
    'string.min': 'Medication name must be at least 2 characters long',
    'string.max': 'Medication name must not exceed 255 characters',
    'any.required': 'Medication name is required'
  }),
  dosage: Joi.string().min(1).max(100).required().messages({
    'string.min': 'Dosage must be specified',
    'string.max': 'Dosage must not exceed 100 characters',
    'any.required': 'Dosage is required'
  }),
  set_id: Joi.string().max(100).optional(),
  ndc: Joi.string().max(50).optional(),
  instructions: Joi.string().max(1000).optional(),
  frequency: Joi.string().max(100).default('Every day'),
  doctor_name: Joi.string().max(255).optional(),
  pharmacy_name: Joi.string().max(255).optional(),
  pharmacy_location: Joi.string().max(500).optional(),
  quantity_total: Joi.number().integer().min(1).optional().messages({
    'number.min': 'Quantity must be at least 1',
    'number.integer': 'Quantity must be a whole number'
  }),
  quantity_remaining: Joi.number().integer().min(0).optional().messages({
    'number.min': 'Remaining quantity cannot be negative',
    'number.integer': 'Remaining quantity must be a whole number'
  }),
  next_refill_date: Joi.date().iso().min('now').optional().messages({
    'date.iso': 'Refill date must be in ISO format (YYYY-MM-DD)',
    'date.min': 'Refill date cannot be in the past'
  }),
  refill_reminder_days: Joi.number().integer().min(1).max(90).default(7).messages({
    'number.min': 'Reminder days must be at least 1',
    'number.max': 'Reminder days must not exceed 90'
  }),
  start_date: Joi.date().iso().optional().messages({
    'date.iso': 'Start date must be in ISO format (YYYY-MM-DD)'
  }),
  end_date: Joi.date().iso().optional().messages({
    'date.iso': 'End date must be in ISO format (YYYY-MM-DD)'
  }),
  color: Joi.string().pattern(/^#[0-9A-Fa-f]{6}$/).default('#4CAF50').messages({
    'string.pattern.base': 'Color must be a valid hex color code (e.g., #4CAF50)'
  }),
  schedules: Joi.array().items(scheduleSchema).min(1).optional().messages({
    'array.min': 'At least one schedule must be provided if schedules array is included'
  })
}).custom((value, helpers) => {
  if (value.quantity_remaining && value.quantity_total && value.quantity_remaining > value.quantity_total) {
    return helpers.error('quantity.remaining.exceeded');
  }
  if (value.end_date && value.start_date && new Date(value.end_date) < new Date(value.start_date)) {
    return helpers.error('date.end.before.start');
  }
  return value;
}).messages({
  'quantity.remaining.exceeded': 'Remaining quantity cannot exceed total quantity',
  'date.end.before.start': 'End date cannot be before start date'
});

export const updateUserMedicationSchema = Joi.object({
  medication_name: Joi.string().min(2).max(255).optional(),
  dosage: Joi.string().min(1).max(100).optional(),
  instructions: Joi.string().max(1000).optional().allow('', null),
  frequency: Joi.string().max(100).optional(),
  doctor_name: Joi.string().max(255).optional().allow('', null),
  pharmacy_name: Joi.string().max(255).optional().allow('', null),
  pharmacy_location: Joi.string().max(500).optional().allow('', null),
  quantity_total: Joi.number().integer().min(1).optional(),
  quantity_remaining: Joi.number().integer().min(0).optional(),
  next_refill_date: Joi.date().iso().optional().allow(null),
  refill_reminder_days: Joi.number().integer().min(1).max(90).optional(),
  start_date: Joi.date().iso().optional().allow(null),
  is_active: Joi.boolean().optional(),
  end_date: Joi.date().iso().optional().allow(null),
  color: Joi.string().pattern(/^#[0-9A-Fa-f]{6}$/).optional()
}).min(1).messages({
  'object.min': 'At least one field must be provided to update'
});

export const createScheduleSchema = scheduleSchema;

export const updateScheduleSchema = Joi.object({
  scheduled_time: Joi.string().pattern(/^([01]\d|2[0-3]):([0-5]\d)(:([0-5]\d))?$/).optional().messages({
    'string.pattern.base': 'Scheduled time must be in HH:mm or HH:mm:ss format'
  }),
  days_of_week: Joi.array().items(Joi.number().min(0).max(6)).min(1).max(7).optional(),
  is_enabled: Joi.boolean().optional()
}).min(1).messages({
  'object.min': 'At least one field must be provided to update'
});

export const markMedicationSchema = Joi.object({
  status: Joi.string().valid('taken', 'skipped').required().messages({
    'any.only': 'Status must be either "taken" or "skipped"',
    'any.required': 'Status is required'
  }),
  taken_at: Joi.date().iso().optional().messages({
    'date.iso': 'Taken at must be in ISO format'
  }),
  notes: Joi.string().max(500).optional()
});