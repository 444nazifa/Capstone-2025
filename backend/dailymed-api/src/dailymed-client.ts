import axios, { AxiosInstance } from 'axios';
import { 
  MedicationSearchResult, 
  MedicationDetails, 
  SearchParams, 
  NDCSearchParams, 
  AutocompleteParams,
  AutocompleteResult,
  ApiResponse,
  ActiveIngredient,
  MedicationImage
} from './types';

export class DailyMedClient {
  private client: AxiosInstance;
  private baseURL = 'https://dailymed.nlm.nih.gov/dailymed';

  constructor() {
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: 30000,
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'DailyMed-API-Client/1.0.0'
      }
    });
  }

  async searchByMedicationName(params: SearchParams): Promise<ApiResponse<MedicationSearchResult[]>> {
    try {
      const { query, limit = 20, offset = 0 } = params;
      
      const response = await this.client.get('/services/v2/spls.json', {
        params: {
          drug_name: query,
          pagesize: limit,
          page: Math.floor(offset / limit) + 1
        }
      });

      const medications: MedicationSearchResult[] = await Promise.all(
        response.data.data.map(async (item: any) => {
          const [ndcResponse, mediaResponse, detailResponse] = await Promise.allSettled([
            this.client.get(`/services/v2/spls/${item.setid}/ndcs.json`),
            this.client.get(`/services/v2/spls/${item.setid}/media.json`),
            this.client.get(`/services/v2/spls/${item.setid}.json`)
          ]);

          let ndc: string[] = [];
          let image: string | undefined;
          let labeler = '';

          console.log(`ndcResponse.value.data`, ndcResponse);

          if (ndcResponse.status === 'fulfilled') {
            ndc = ndcResponse.value.data?.ndc || [];
          }

          if (mediaResponse.status === 'fulfilled') {
            const media = mediaResponse.value.data?.media || [];
            image = media.find((m: any) => m.type === 'product')?.url || media[0]?.url;
          }

          if (detailResponse.status === 'fulfilled') {
            labeler = detailResponse.value.data?.labeler || '';
          }

          return {
            setId: item.setid,
            title: item.title,
            ndc,
            image,
            labeler,
            published: item.published,
            updated: item.updated
          };
        })
      );

      return {
        success: true,
        data: medications,
        total: response.data.metadata?.total || medications.length
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }

  async searchByNDC(params: NDCSearchParams): Promise<ApiResponse<MedicationSearchResult[]>> {
    try {
      const { ndc } = params;
      
      const response = await this.client.get('/services/v2/spls.json', {
        params: {
          ndc: ndc
        }
      });

      const medications: MedicationSearchResult[] = await Promise.all(
        response.data.data.map(async (item: any) => {
          const [ndcResponse, mediaResponse, detailResponse] = await Promise.allSettled([
            this.client.get(`/spls/${item.setid}/ndcs`),
            this.client.get(`/spls/${item.setid}/media`),
            this.client.get(`/services/v2/spls/${item.setid}.json`)
          ]);

          let ndcList: string[] = [];
          let image: string | undefined;
          let labeler = '';

          if (ndcResponse.status === 'fulfilled') {
            ndcList = ndcResponse.value.data?.ndc || [];
          }

          if (mediaResponse.status === 'fulfilled') {
            const media = mediaResponse.value.data?.media || [];
            image = media.find((m: any) => m.type === 'product')?.url || media[0]?.url;
          }

          if (detailResponse.status === 'fulfilled') {
            labeler = detailResponse.value.data?.labeler || '';
          }

          return {
            setId: item.setid,
            title: item.title,
            ndc: ndcList,
            image,
            labeler,
            published: item.published,
            updated: item.updated
          };
        })
      );

      return {
        success: true,
        data: medications,
        total: response.data.metadata?.total || medications.length
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }

  async getMedicationDetails(setId: string): Promise<ApiResponse<MedicationDetails>> {
    try {
      const [detailResponse, ndcResponse, mediaResponse, packagingResponse] = await Promise.allSettled([
        this.client.get(`/services/v2/spls/${setId}.json`),
        this.client.get(`/spls/${setId}/ndcs`),
        this.client.get(`/spls/${setId}/media`),
        this.client.get(`/spls/${setId}/packaging`)
      ]);

      if (detailResponse.status === 'rejected') {
        throw new Error('Failed to fetch medication details');
      }

      const data = detailResponse.value.data;

      let ndc: string[] = [];
      let images: MedicationImage[] = [];
      let packageLabel: string[] = [];

      if (ndcResponse.status === 'fulfilled') {
        ndc = ndcResponse.value.data?.ndc || [];
      }

      if (mediaResponse.status === 'fulfilled') {
        const media = mediaResponse.value.data?.media || [];
        images = media.map((img: any) => ({
          url: img.url,
          caption: img.caption,
          type: img.type || 'other'
        }));
      }

      if (packagingResponse.status === 'fulfilled') {
        packageLabel = packagingResponse.value.data?.packaging || [];
      }

      const activeIngredients: ActiveIngredient[] = (data.active_ingredients || []).map((ingredient: any) => ({
        name: ingredient.name,
        strength: ingredient.strength || ''
      }));

      const medicationDetails: MedicationDetails = {
        setId: data.setid,
        title: data.title,
        ndc,
        labeler: data.labeler,
        published: data.published,
        updated: data.updated,
        description: data.description,
        activeIngredients,
        inactiveIngredients: data.inactive_ingredients || [],
        dosageForm: data.dosage_form || '',
        strength: data.strength || '',
        route: data.route || [],
        indicationsAndUsage: data.indications_and_usage,
        dosageAndAdministration: data.dosage_and_administration,
        contraindications: data.contraindications,
        warnings: data.warnings,
        adverseReactions: data.adverse_reactions,
        drugInteractions: data.drug_interactions,
        useInSpecificPopulations: data.use_in_specific_populations,
        overdosage: data.overdosage,
        clinicalPharmacology: data.clinical_pharmacology,
        howSupplied: data.how_supplied,
        storageAndHandling: data.storage_and_handling,
        patientCounselingInfo: data.patient_counseling_info,
        images,
        packageLabel
      };

      return {
        success: true,
        data: medicationDetails
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }

  async autocomplete(params: AutocompleteParams): Promise<ApiResponse<AutocompleteResult[]>> {
    try {
      const { term, limit = 10 } = params;
      
      const response = await this.client.get('/autocomplete.cfm', {
        params: {
          key: 'search',
          returntype: 'json',
          term: term,
          limit: limit
        }
      });

      const suggestions: AutocompleteResult[] = (response.data || []).map((item: any) => ({
        label: item.label || item,
        value: item.value || item
      }));

      return {
        success: true,
        data: suggestions,
        total: suggestions.length
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }
}