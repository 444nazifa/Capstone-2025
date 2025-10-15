import { Router, Request, Response } from 'express';
import { DailyMedClient } from './dailymed-client';
import { SearchParams, NDCSearchParams, AutocompleteParams } from './types';

const router = Router();
const dailyMedClient = new DailyMedClient();

router.get('/search/medication', async (req: Request, res: Response) => {
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

    const result = await dailyMedClient.searchByMedicationName(searchParams);
    
    if (result.success) {
      res.json(result);
    } else {
      res.status(500).json(result);
    }
  } catch (error) {
    res.status(500).json({
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
    const result = await dailyMedClient.searchByNDC(searchParams);
    
    if (result.success) {
      res.json(result);
    } else {
      res.status(500).json(result);
    }
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

router.get('/medication/:setId', async (req: Request, res: Response) => {
  try {
    const { setId } = req.params;

    if (!setId) {
      return res.status(400).json({
        success: false,
        error: 'Set ID parameter is required'
      });
    }

    const result = await dailyMedClient.getMedicationDetails(setId);
    
    if (result.success) {
      res.json(result);
    } else {
      res.status(500).json(result);
    }
  } catch (error) {
    res.status(500).json({
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

    const result = await dailyMedClient.autocomplete(autocompleteParams);
    
    if (result.success) {
      res.json(result);
    } else {
      res.status(500).json(result);
    }
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error instanceof Error ? error.message : 'Internal server error'
    });
  }
});

export default router;