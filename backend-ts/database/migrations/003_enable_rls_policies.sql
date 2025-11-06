-- Migration: Enable Row Level Security (RLS) on medication tables

-- Enable RLS on all medication tables
ALTER TABLE user_medications ENABLE ROW LEVEL SECURITY;
ALTER TABLE medication_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE medication_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE drug_interactions ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- USER_MEDICATIONS POLICIES
-- =====================================================

-- Allow users to view only their own medications
CREATE POLICY "Users can view their own medications"
ON user_medications
FOR SELECT
USING (auth.uid() = user_id);

-- Allow users to insert their own medications
CREATE POLICY "Users can insert their own medications"
ON user_medications
FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Allow users to update their own medications
CREATE POLICY "Users can update their own medications"
ON user_medications
FOR UPDATE
USING (auth.uid() = user_id);

-- Allow users to delete their own medications
CREATE POLICY "Users can delete their own medications"
ON user_medications
FOR DELETE
USING (auth.uid() = user_id);

-- =====================================================
-- MEDICATION_SCHEDULES POLICIES
-- =====================================================

-- Allow users to view schedules for their medications
CREATE POLICY "Users can view their medication schedules"
ON medication_schedules
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE user_medications.id = medication_schedules.user_medication_id
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to insert schedules for their medications
CREATE POLICY "Users can insert schedules for their medications"
ON medication_schedules
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE user_medications.id = medication_schedules.user_medication_id
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to update schedules for their medications
CREATE POLICY "Users can update their medication schedules"
ON medication_schedules
FOR UPDATE
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE user_medications.id = medication_schedules.user_medication_id
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to delete schedules for their medications
CREATE POLICY "Users can delete their medication schedules"
ON medication_schedules
FOR DELETE
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE user_medications.id = medication_schedules.user_medication_id
    AND user_medications.user_id = auth.uid()
  )
);

-- =====================================================
-- MEDICATION_HISTORY POLICIES
-- =====================================================

-- Allow users to view their own medication history
CREATE POLICY "Users can view their own medication history"
ON medication_history
FOR SELECT
USING (auth.uid() = user_id);

-- Allow users to insert their own medication history
CREATE POLICY "Users can insert their own medication history"
ON medication_history
FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Allow users to update their own medication history
CREATE POLICY "Users can update their own medication history"
ON medication_history
FOR UPDATE
USING (auth.uid() = user_id);

-- Allow users to delete their own medication history
CREATE POLICY "Users can delete their own medication history"
ON medication_history
FOR DELETE
USING (auth.uid() = user_id);

-- =====================================================
-- DRUG_INTERACTIONS POLICIES
-- =====================================================

-- Allow users to view interactions for their medications
CREATE POLICY "Users can view their drug interactions"
ON drug_interactions
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE (user_medications.id = drug_interactions.medication_a_id
       OR user_medications.id = drug_interactions.medication_b_id)
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to insert interactions for their medications
CREATE POLICY "Users can insert drug interactions"
ON drug_interactions
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE (user_medications.id = drug_interactions.medication_a_id
       OR user_medications.id = drug_interactions.medication_b_id)
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to update interactions for their medications
CREATE POLICY "Users can update their drug interactions"
ON drug_interactions
FOR UPDATE
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE (user_medications.id = drug_interactions.medication_a_id
       OR user_medications.id = drug_interactions.medication_b_id)
    AND user_medications.user_id = auth.uid()
  )
);

-- Allow users to delete interactions for their medications
CREATE POLICY "Users can delete their drug interactions"
ON drug_interactions
FOR DELETE
USING (
  EXISTS (
    SELECT 1 FROM user_medications
    WHERE (user_medications.id = drug_interactions.medication_a_id
       OR user_medications.id = drug_interactions.medication_b_id)
    AND user_medications.user_id = auth.uid()
  )
);
