export interface MedicationSearchResult {
  setId: string;
  title: string;
  ndc: string[];
  image?: string;
  labeler: string;
  published: string;
  updated: string;
}

export interface MedicationDetails {
  setId: string;
  title: string;
  ndc: string[];
  labeler: string;
  published: string;
  updated: string;
  description?: string;
  activeIngredients: ActiveIngredient[];
  inactiveIngredients: string[];
  dosageForm: string;
  strength: string;
  route: string[];
  indicationsAndUsage?: string;
  dosageAndAdministration?: string;
  contraindications?: string;
  warnings?: string;
  adverseReactions?: string;
  drugInteractions?: string;
  useInSpecificPopulations?: string;
  overdosage?: string;
  clinicalPharmacology?: string;
  howSupplied?: string;
  storageAndHandling?: string;
  patientCounselingInfo?: string;
  images: MedicationImage[];
  packageLabel: string[];
}

export interface ActiveIngredient {
  name: string;
  strength: string;
}

export interface MedicationImage {
  url: string;
  caption?: string;
  type: 'product' | 'label' | 'other';
}

export interface SearchParams {
  query: string;
  limit?: number;
  offset?: number;
}

export interface NDCSearchParams {
  ndc: string;
}

export interface AutocompleteResult {
  label: string;
  value: string;
}

export interface AutocompleteParams {
  term: string;
  limit?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  total?: number;
}

export interface UserMedication {
  id: string;
  user_id: string;
  medication_name: string;
  dosage: string;
  set_id?: string;
  ndc?: string;
  instructions?: string;
  frequency: string;
  doctor_name?: string;
  pharmacy_name?: string;
  pharmacy_location?: string;
  quantity_total?: number;
  quantity_remaining?: number;
  supply_remaining_percentage?: number;
  next_refill_date?: string;
  refill_reminder_days?: number;
  is_active: boolean;
  start_date: string;
  end_date?: string;
  color: string;
  created_at: string;
  updated_at: string;
  schedules?: MedicationSchedule[];
}

export interface MedicationSchedule {
  id: string;
  user_medication_id: string;
  scheduled_time: string;
  days_of_week: number[];
  is_enabled: boolean;
  created_at: string;
  updated_at: string;
}

export interface MedicationHistory {
  id: string;
  user_id: string;
  user_medication_id: string;
  medication_schedule_id?: string;
  scheduled_at: string;
  taken_at?: string;
  status: 'taken' | 'skipped' | 'missed' | 'pending';
  notes?: string;
  created_at: string;
}

export interface DrugInteraction {
  id: string;
  medication_a_id: string;
  medication_b_id: string;
  severity: 'mild' | 'moderate' | 'severe';
  description: string;
  recommendation?: string;
  is_acknowledged: boolean;
  acknowledged_at?: string;
  created_at: string;
  medication_a?: UserMedication;
  medication_b?: UserMedication;
}

export interface CreateUserMedicationRequest {
  medication_name: string;
  dosage: string;
  set_id?: string;
  ndc?: string;
  instructions?: string;
  frequency: string;
  doctor_name?: string;
  pharmacy_name?: string;
  pharmacy_location?: string;
  quantity_total?: number;
  quantity_remaining?: number;
  next_refill_date?: string;
  refill_reminder_days?: number;
  start_date?: string;
  end_date?: string;
  color?: string;
  schedules?: CreateScheduleRequest[];
}

export interface UpdateUserMedicationRequest {
  medication_name?: string;
  dosage?: string;
  instructions?: string;
  frequency?: string;
  doctor_name?: string;
  pharmacy_name?: string;
  pharmacy_location?: string;
  quantity_total?: number;
  quantity_remaining?: number;
  next_refill_date?: string;
  refill_reminder_days?: number;
  is_active?: boolean;
  end_date?: string;
  color?: string;
}

export interface CreateScheduleRequest {
  scheduled_time: string;
  days_of_week?: number[];
  is_enabled?: boolean;
}

export interface UpdateScheduleRequest {
  scheduled_time?: string;
  days_of_week?: number[];
  is_enabled?: boolean;
}

export interface MarkMedicationRequest {
  status: 'taken' | 'skipped';
  taken_at?: string;
  notes?: string;
}

export interface MedicationSummary {
  total_active: number;
  low_supply_count: number;
  upcoming_refills: number;
  medications_due_today: number;
  adherence_rate?: number;
}

export interface DailyMedicationSchedule {
  date: string;
  medications: {
    medication: UserMedication;
    schedule: MedicationSchedule;
    history?: MedicationHistory;
  }[];
}

export interface UserMedicationResponse {
  success: boolean;
  message: string;
  medication?: UserMedication;
  medications?: UserMedication[];
  summary?: MedicationSummary;
  interactions?: DrugInteraction[];
  error?: string;
}

export interface MedicationHistoryResponse {
  success: boolean;
  message?: string;
  history?: MedicationHistory[];
  total?: number;
  error?: string;
}