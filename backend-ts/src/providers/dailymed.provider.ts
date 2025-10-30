import axios, { AxiosResponse } from 'axios';
import { SearchParams, NDCSearchParams, AutocompleteParams, ApiResponse, MedicationSearchResult, MedicationDetails, AutocompleteResult } from '../types/medication';

export class DailyMedProvider {
  private readonly baseUrl = 'https://dailymed.nlm.nih.gov/dailymed/services/v2';

  async searchByMedicationName(params: SearchParams): Promise<ApiResponse<MedicationSearchResult[]>> {
    try {
      const { query, limit = 20, offset = 0 } = params;
      
      const searchUrl = `${this.baseUrl}/spls.json`;
      const searchParams = {
        drug_name: query,
        page_size: limit.toString(),
        page_number: Math.floor(offset / limit) + 1
      };

      const response: AxiosResponse = await axios.get(searchUrl, {
        params: searchParams,
        timeout: 10000
      });

      if (!response.data || !response.data.data) {
        return {
          success: false,
          error: 'Invalid response format from DailyMed API'
        };
      }

      const medications: MedicationSearchResult[] = await Promise.all(
        response.data.data.map(async (item: any) => {
          // Extract labeler from title (it's usually in brackets at the end)
          const titleMatch = item.title.match(/\[([^\]]+)\]$/);
          const labeler = titleMatch ? titleMatch[1] : 'Unknown';

          // Try to get first image for this medication
          let image: string | undefined;
          try {
            const mediaUrl = `${this.baseUrl}/spls/${item.setid}/media.json`;
            const mediaResponse = await axios.get(mediaUrl, { timeout: 5000 });
            if (mediaResponse.data?.data?.media && Array.isArray(mediaResponse.data.data.media) && mediaResponse.data.data.media.length > 0) {
              image = mediaResponse.data.data.media[0].url || undefined;
              console.log(`Found image for ${item.setid}: ${image}`);
            } else {
              console.log(`No images available for medication ${item.setid}`);
            }
          } catch (mediaError) {
            // Images not critical for search results
            console.log(`Failed to fetch images for medication ${item.setid}:`, mediaError instanceof Error ? mediaError.message : 'Unknown error');
          }

          return {
            setId: item.setid,
            title: item.title,
            ndc: [], // NDC data not available in search results
            image: image,
            labeler: labeler,
            published: item.published_date || '',
            updated: item.published_date || ''
          };
        })
      );

      return {
        success: true,
        data: medications,
        total: response.data.metadata?.total || medications.length
      };
    } catch (error) {
      console.error('DailyMed search error:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to search medications'
      };
    }
  }

  async searchByNDC(params: NDCSearchParams): Promise<ApiResponse<MedicationSearchResult[]>> {
    try {
      let { ndc } = params;

      // Remove any existing dashes
      ndc = ndc.replace(/-/g, '');

      // Convert 11-digit NDC to 10-digit format and add dashes
      // Format: XXXXX-XXXX-XX (11 digits) -> XXXXX-XXX-XX (10 digits with dashes)
      let formattedNDC: string;
      if (ndc.length === 11) {
        // 5-4-2 format
        const labeler = ndc.slice(0, 5);
        const product = ndc.slice(5, 9);
        const package_code = ndc.slice(9, 11);

        // Remove leading zero from product code to make it 10-digit
        const trimmedProduct = product.startsWith('0') ? product.slice(1) : product;
        formattedNDC = `${labeler}-${trimmedProduct}-${package_code}`;
      } else if (ndc.length === 10) {
        // Already 10 digits, just add dashes in 5-3-2 format
        formattedNDC = `${ndc.slice(0, 5)}-${ndc.slice(5, 8)}-${ndc.slice(8, 10)}`;
      } else {
        // Try as-is
        formattedNDC = ndc;
      }

      const searchUrl = `${this.baseUrl}/spls.json`;
      const searchParams = {
        ndc: formattedNDC
      };

      const response: AxiosResponse = await axios.get(searchUrl, {
        params: searchParams,
        timeout: 10000
      });

      if (!response.data || !response.data.data) {
        return {
          success: false,
          error: 'Invalid response format from DailyMed API'
        };
      }

      const medications: MedicationSearchResult[] = response.data.data.map((item: any) => ({
        setId: item.setid,
        title: item.title,
        ndc: item.openfda?.ndc || [],
        labeler: item.openfda?.manufacturer_name?.[0] || 'Unknown',
        published: item.published,
        updated: item.updated
      }));

      return {
        success: true,
        data: medications,
        total: response.data.metadata?.total || medications.length
      };
    } catch (error) {
      console.error('DailyMed NDC search error:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to search by NDC'
      };
    }
  }

  async getMedicationDetails(setId: string): Promise<ApiResponse<MedicationDetails>> {
    try {
      // Get NDC data
      const ndcUrl = `${this.baseUrl}/spls/${setId}/ndcs.json`;
      let ndcData: string[] = [];
      try {
        const ndcResponse = await axios.get(ndcUrl, { timeout: 10000 });
        ndcData = Array.isArray(ndcResponse.data) ? ndcResponse.data : [];
      } catch (ndcError) {
        // NDC data not critical
      }

      // Get media/images data
      const mediaUrl = `${this.baseUrl}/spls/${setId}/media.json`;
      let mediaData: any[] = [];
      try {
        const mediaResponse = await axios.get(mediaUrl, { timeout: 10000 });
        mediaData = mediaResponse.data?.data?.media && Array.isArray(mediaResponse.data.data.media) ? mediaResponse.data.data.media : [];
      } catch (mediaError) {
        // Media data not critical
      }

      // Get packaging data
      const packagingUrl = `${this.baseUrl}/spls/${setId}/packaging.json`;
      let packagingData: any[] = [];
      try {
        const packagingResponse = await axios.get(packagingUrl, { timeout: 10000 });
        packagingData = Array.isArray(packagingResponse.data) ? packagingResponse.data : [];
      } catch (packagingError) {
        // Packaging data not critical
      }

      // Get basic info from search
      let title = 'Unknown Medication';
      let labeler = 'Unknown';
      let published = '';
      
      // Initialize with fallback values
      let description = 'Detailed medication information not available';
      let indicationsAndUsage = 'Indications and usage information not available';
      let adverseReactions = 'Adverse reactions information not available';
      let warnings = 'Warnings and precautions information not available';
      let contraindications = 'Contraindications information not available';
      let dosageAndAdministration = 'Dosage and administration information not available';

      // Try to fetch detailed information from DailyMed's drug info page
      try {
        console.log(`Fetching detailed medication info for ${setId}...`);
        const drugInfoUrl = `https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=${setId}`;
        const drugInfoResponse = await axios.get(drugInfoUrl, { 
          timeout: 15000,
          headers: {
            'User-Agent': 'Mozilla/5.0 (compatible; MedicationAPI/1.0)'
          }
        });

        const htmlContent = drugInfoResponse.data;
        
        // Extract sections using regex patterns
        const extractSection = (sectionName: string): string => {
          // Look for section headers and extract content
          const patterns = [
            new RegExp(`<[^>]*>${sectionName}[^<]*</[^>]*>([\\s\\S]*?)(?=<[^>]*>(?:DOSAGE|CONTRAINDICATIONS|WARNINGS|ADVERSE|INDICATIONS|CLINICAL|HOW SUPPLIED|DESCRIPTION)[^<]*</[^>]*>|$)`, 'i'),
            new RegExp(`<h[1-6][^>]*[^>]*>${sectionName}[^<]*</h[1-6]>([\\s\\S]*?)(?=<h[1-6]|$)`, 'i'),
            new RegExp(`${sectionName}\\s*:?\\s*</[^>]*>([\\s\\S]*?)(?=(?:DOSAGE|CONTRAINDICATIONS|WARNINGS|ADVERSE|INDICATIONS|CLINICAL|HOW SUPPLIED|DESCRIPTION)\\s*:?\\s*</[^>]*>|$)`, 'i')
          ];

          for (const pattern of patterns) {
            const match = htmlContent.match(pattern);
            if (match && match[1]) {
              // Clean up HTML tags and normalize whitespace
              return match[1]
                .replace(/<[^>]*>/g, ' ')
                .replace(/&nbsp;/g, ' ')
                .replace(/&amp;/g, '&')
                .replace(/&lt;/g, '<')
                .replace(/&gt;/g, '>')
                .replace(/&quot;/g, '"')
                .replace(/&#39;/g, "'")
                .replace(/\s+/g, ' ')
                .trim()
                .substring(0, 2000); // Limit length
            }
          }
          return '';
        };

        // Extract specific sections
        const extractedIndications = extractSection('INDICATIONS AND USAGE');
        const extractedAdverse = extractSection('ADVERSE REACTIONS');
        const extractedWarnings = extractSection('WARNINGS AND PRECAUTIONS|WARNINGS');
        const extractedContraindications = extractSection('CONTRAINDICATIONS');
        const extractedDosage = extractSection('DOSAGE AND ADMINISTRATION');
        const extractedDescription = extractSection('DESCRIPTION');

        // Update values if extraction was successful
        if (extractedIndications) {
          indicationsAndUsage = extractedIndications;
          console.log(`Extracted indications and usage for ${setId}`);
        }
        if (extractedAdverse) {
          adverseReactions = extractedAdverse;
          console.log(`Extracted adverse reactions for ${setId}`);
        }
        if (extractedWarnings) {
          warnings = extractedWarnings;
          console.log(`Extracted warnings for ${setId}`);
        }
        if (extractedContraindications) {
          contraindications = extractedContraindications;
          console.log(`Extracted contraindications for ${setId}`);
        }
        if (extractedDosage) {
          dosageAndAdministration = extractedDosage;
          console.log(`Extracted dosage and administration for ${setId}`);
        }
        if (extractedDescription) {
          description = extractedDescription;
          console.log(`Extracted description for ${setId}`);
        }

        console.log(`Successfully extracted detailed medication info for ${setId}`);

      } catch (htmlError) {
        console.log(`Failed to fetch detailed medication info for ${setId}:`, htmlError instanceof Error ? htmlError.message : 'Unknown error');
        
        // Provide fallback links
        const baseUrl = 'https://dailymed.nlm.nih.gov/dailymed';
        description = `Complete prescribing information available at: ${baseUrl}/drugInfo.cfm?setid=${setId}`;
        indicationsAndUsage = `Full prescribing information available via PDF: ${baseUrl}/downloadpdffile.cfm?setId=${setId}`;
        adverseReactions = `Adverse reactions information available in full prescribing information: ${baseUrl}/downloadpdffile.cfm?setId=${setId}`;
        warnings = `Warnings and precautions available in complete product labeling: ${baseUrl}/downloadpdffile.cfm?setId=${setId}`;
        contraindications = `Contraindications listed in full prescribing information: ${baseUrl}/downloadpdffile.cfm?setId=${setId}`;
        dosageAndAdministration = `Dosage and administration guidelines available in complete product labeling: ${baseUrl}/downloadpdffile.cfm?setId=${setId}`;
      }
      
      try {
        const searchResponse = await axios.get(`${this.baseUrl}/spls.json`, {
          params: { setid: setId },
          timeout: 10000
        });
        
        if (searchResponse.data?.data?.[0]) {
          const item = searchResponse.data.data[0];
          title = item.title || title;
          published = item.published_date || '';
          const titleMatch = title.match(/\[([^\]]+)\]$/);
          labeler = titleMatch ? titleMatch[1] || 'Unknown' : 'Unknown';
        }
      } catch (searchError) {
        // Use fallback values
      }

      const medication: MedicationDetails = {
        setId: setId,
        title: title,
        ndc: ndcData,
        labeler: labeler,
        published: published,
        updated: published,
        description: description,
        activeIngredients: [],
        inactiveIngredients: [],
        dosageForm: 'Not specified',
        strength: 'Not specified',
        route: [],
        indicationsAndUsage: indicationsAndUsage,
        dosageAndAdministration: dosageAndAdministration,
        contraindications: contraindications,
        warnings: warnings,
        adverseReactions: adverseReactions,
        drugInteractions: 'Please refer to package labeling for drug interactions',
        useInSpecificPopulations: 'Please refer to package labeling for use in specific populations',
        overdosage: 'Please refer to package labeling for overdosage information',
        clinicalPharmacology: 'Please refer to package labeling for clinical pharmacology',
        howSupplied: packagingData.length > 0 ? packagingData.map(p => p.description || '').join(', ') : 'Not specified',
        storageAndHandling: 'Please refer to package labeling for storage information',
        patientCounselingInfo: 'Please refer to package labeling for patient counseling information',
        images: mediaData.map((media: any) => ({
          url: media.url || '',
          caption: media.caption || '',
          type: media.subType === 'label' ? 'label' : 'product'
        })),
        packageLabel: []
      };

      return {
        success: true,
        data: medication
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to get medication details'
      };
    }
  }

  async autocomplete(params: AutocompleteParams): Promise<ApiResponse<AutocompleteResult[]>> {
    try {
      const { term, limit = 10 } = params;
      
      // Use search to get autocomplete suggestions by searching for the term
      const searchUrl = `${this.baseUrl}/spls.json`;
      const searchParams = {
        drug_name: term,
        page_size: (limit * 2).toString(), // Get more results to have variety
        page_number: 1
      };

      const response: AxiosResponse = await axios.get(searchUrl, {
        params: searchParams,
        timeout: 5000
      });

      if (!response.data || !response.data.data) {
        return {
          success: false,
          error: 'No autocomplete suggestions found'
        };
      }

      // Extract unique drug names from titles
      const suggestions: Set<string> = new Set();
      
      response.data.data.forEach((item: any) => {
        const title = item.title || '';
        // Extract the main drug name (usually the first part before parentheses)
        const mainDrugMatch = title.match(/^([A-Z][A-Z\s]+?)(?:\s+\(|$)/);
        if (mainDrugMatch) {
          const drugName = mainDrugMatch[1].trim();
          if (drugName.toLowerCase().includes(term.toLowerCase())) {
            suggestions.add(drugName);
          }
        }
        
        // Also add the full medication name if it's short enough
        if (title.length < 50 && title.toLowerCase().includes(term.toLowerCase())) {
          suggestions.add(title);
        }
      });

      const autocompleteResults: AutocompleteResult[] = Array.from(suggestions)
        .slice(0, limit)
        .map((name: string) => ({
          label: name,
          value: name
        }));

      return {
        success: true,
        data: autocompleteResults
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to get autocomplete suggestions'
      };
    }
  }
}