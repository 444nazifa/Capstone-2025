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