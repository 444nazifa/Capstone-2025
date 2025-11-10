CREATE TABLE IF NOT EXISTS user_medications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,

  medication_name VARCHAR(255) NOT NULL,
  dosage VARCHAR(100) NOT NULL,
  set_id TEXT,
  ndc VARCHAR(50),

  instructions TEXT,
  frequency VARCHAR(100) NOT NULL DEFAULT 'Every day',

  doctor_name VARCHAR(255),
  pharmacy_name VARCHAR(255),
  pharmacy_location VARCHAR(500),

  quantity_total INTEGER,
  quantity_remaining INTEGER,
  supply_remaining_percentage DECIMAL(5,2) GENERATED ALWAYS AS (
    CASE
      WHEN quantity_total > 0 THEN (quantity_remaining::DECIMAL / quantity_total::DECIMAL) * 100
      ELSE 0
    END
  ) STORED,
  next_refill_date DATE,
  refill_reminder_days INTEGER DEFAULT 7,

  is_active BOOLEAN DEFAULT true,
  start_date DATE DEFAULT CURRENT_DATE,
  end_date DATE,

  color VARCHAR(7) DEFAULT '#4CAF50',

  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

  CONSTRAINT valid_quantity CHECK (quantity_remaining >= 0 AND quantity_remaining <= quantity_total),
  CONSTRAINT valid_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS medication_schedules (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_medication_id UUID NOT NULL REFERENCES user_medications(id) ON DELETE CASCADE,

  scheduled_time TIME NOT NULL,
  days_of_week INTEGER[] DEFAULT ARRAY[0,1,2,3,4,5,6],

  is_enabled BOOLEAN DEFAULT true,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

  CONSTRAINT valid_days_of_week CHECK (
    array_length(days_of_week, 1) > 0 AND
    days_of_week <@ ARRAY[0,1,2,3,4,5,6]
  )
);

CREATE TABLE IF NOT EXISTS medication_history (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  user_medication_id UUID NOT NULL REFERENCES user_medications(id) ON DELETE CASCADE,
  medication_schedule_id UUID REFERENCES medication_schedules(id) ON DELETE SET NULL,

  scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
  taken_at TIMESTAMP WITH TIME ZONE,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',

  notes TEXT,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

  CONSTRAINT valid_status CHECK (status IN ('taken', 'skipped', 'missed', 'pending'))
);

CREATE TABLE IF NOT EXISTS drug_interactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  medication_a_id UUID NOT NULL REFERENCES user_medications(id) ON DELETE CASCADE,
  medication_b_id UUID NOT NULL REFERENCES user_medications(id) ON DELETE CASCADE,

  severity VARCHAR(20) NOT NULL,
  description TEXT NOT NULL,
  recommendation TEXT,

  is_acknowledged BOOLEAN DEFAULT false,
  acknowledged_at TIMESTAMP WITH TIME ZONE,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

  CONSTRAINT different_medications CHECK (medication_a_id != medication_b_id),
  CONSTRAINT valid_severity CHECK (severity IN ('mild', 'moderate', 'severe')),
  CONSTRAINT unique_interaction UNIQUE (medication_a_id, medication_b_id)
);

CREATE INDEX idx_user_medications_user_id ON user_medications(user_id);
CREATE INDEX idx_user_medications_active ON user_medications(user_id, is_active);
CREATE INDEX idx_user_medications_refill ON user_medications(next_refill_date) WHERE is_active = true;

CREATE INDEX idx_medication_schedules_medication ON medication_schedules(user_medication_id);
CREATE INDEX idx_medication_schedules_enabled ON medication_schedules(user_medication_id, is_enabled);

CREATE INDEX idx_medication_history_user ON medication_history(user_id);
CREATE INDEX idx_medication_history_medication ON medication_history(user_medication_id);
CREATE INDEX idx_medication_history_scheduled ON medication_history(scheduled_at);
CREATE INDEX idx_medication_history_status ON medication_history(user_id, status);

CREATE INDEX idx_drug_interactions_medications ON drug_interactions(medication_a_id, medication_b_id);
CREATE INDEX idx_drug_interactions_severity ON drug_interactions(severity) WHERE is_acknowledged = false;

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_medications_updated_at
  BEFORE UPDATE ON user_medications
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_medication_schedules_updated_at
  BEFORE UPDATE ON medication_schedules
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION decrement_medication_quantity()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'taken' AND (OLD.status IS NULL OR OLD.status != 'taken') THEN
    UPDATE user_medications
    SET quantity_remaining = GREATEST(quantity_remaining - 1, 0)
    WHERE id = NEW.user_medication_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auto_decrement_quantity
  AFTER INSERT OR UPDATE ON medication_history
  FOR EACH ROW EXECUTE FUNCTION decrement_medication_quantity();
