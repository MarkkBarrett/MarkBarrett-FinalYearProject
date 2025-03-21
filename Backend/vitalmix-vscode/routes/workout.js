const express = require('express');
const mongoose = require('mongoose');
const WorkoutPlan = require('../models/WorkoutPlan');
const router = express.Router();

/**
 * @route GET /api/workout/plan
 * @desc Get the logged-in user's workout plan
 * @access Public
 */
router.get('/plan', async (req, res) => {
    const { userId } = req.query;

    if (!userId) {
        return res.status(400).json({ success: false, message: 'User ID is required' });
    }

    try {
        const plan = await WorkoutPlan.findOne({ userId });

        if (!plan) {
            return res.status(404).json({ success: false, message: 'Workout plan not found' });
        }

        res.json({ success: true, data: plan });
    } catch (err) {
        console.error('Error fetching workout plan:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});


// Export the router
module.exports = router;
