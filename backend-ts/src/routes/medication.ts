import { Router, Request, Response } from 'express';
import { DailyMedProvider } from '../providers/dailymed.provider';
import { SearchParams, NDCSearchParams, AutocompleteParams } from '../types/medication';

const router = Router();
const dailyMedProvider = new DailyMedProvider();

router.get('/search', async (req: Request, res: Response) => {
  try {
    const { query, limit, offset } = req.query;

    if (!query || typeof query !== 'string') {
      return res.status(400).json({
        success: false,
        error: 'Query parameter is required and must be a string'
      });
    }

    const searchParams: SearchParams = {
      query,
      limit: limit ? parseInt(limit as string) : undefined,
      offset: offset ? parseInt(offset as string) : undefined
    };

    const result = await dailyMedProvider.searchByMedicationName(searchParams);
    
    if (result.success) {
      return res.json(result);
    } else {
      return res.status(500).json(result);
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

router.get('/search/ndc', async (req: Request, res: Response) => {
  try {
    const { ndc } = req.query;

    if (!ndc || typeof ndc !== 'string') {
      return res.status(400).json({
        success: false,
        error: 'NDC parameter is required and must be a string'
      });
    }

    const searchParams: NDCSearchParams = { ndc };
    const result = await dailyMedProvider.searchByNDC(searchParams);
    
    if (result.success) {
      return res.json(result);
    } else {
      return res.status(500).json(result);
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

router.get('/details/:setId', async (req: Request, res: Response) => {
  try {
    const { setId } = req.params;

    if (!setId) {
      return res.status(400).json({
        success: false,
        error: 'Set ID parameter is required'
      });
    }

    const result = await dailyMedProvider.getMedicationDetails(setId);
    
    if (result.success) {
      return res.json(result);
    } else {
      return res.status(500).json(result);
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

router.get('/autocomplete', async (req: Request, res: Response) => {
  try {
    const { term, limit } = req.query;

    if (!term || typeof term !== 'string') {
      return res.status(400).json({
        success: false,
        error: 'Term parameter is required and must be a string'
      });
    }

    const autocompleteParams: AutocompleteParams = {
      term,
      limit: limit ? parseInt(limit as string) : undefined
    };

    const result = await dailyMedProvider.autocomplete(autocompleteParams);
    
    if (result.success) {
      return res.json(result);
    } else {
      return res.status(500).json(result);
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

export default router;