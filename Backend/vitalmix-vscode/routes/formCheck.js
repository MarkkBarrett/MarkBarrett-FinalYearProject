// routes/formCheck.js
const express = require('express');
const FormResult = require('../models/FormResult');
const router = express.Router();

/**
 * @route GET /api/formResults
 * @desc  Fetch recent form-checker results for a user
 * @access Public
 */
router.get('/formResults', async (req, res) => {
    const { userId, exercise, limit } = req.query;
    if (!userId) {
        return res.status(400).json({ success: false, message: 'userId is required' });
    }
    try {
        const query = { userId };
        if (exercise) query.exercise = exercise;
        const results = await FormResult.find(query).sort({ timestamp: -1 }).limit(parseInt(limit, 10) || 5);
        res.json({ success: true, data: results });
    } catch (err) {
        console.error('Error fetching form results:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

module.exports = router;
