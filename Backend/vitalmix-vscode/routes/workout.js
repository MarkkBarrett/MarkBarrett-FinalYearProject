const express = require('express');
const mongoose = require('mongoose'); //remove and test... unnused 
const WorkoutPlan = require('../models/WorkoutPlan');
const DefaultWorkoutPlan = require('../models/DefaultWorkoutPlan');
const User = require('../models/User');
const router = express.Router();

/**
 * @route GET /api/workout/plan
 * @desc Get the logged-in user's workout plan
 * @access Public
 */

router.get('/plan', async (req, res) => {
    const { userId } = req.query;

    // Check for userId
    if (!userId) {
        return res.status(400).json({ success: false, message: 'User ID is required' });
    }

    // Find assosciated workout plan
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

/**
 * @route GET /api/workout/defaultPlans
 * @desc Fetch all default workout plans
 * @access Public
 */
router.get('/defaultPlans', async (req, res) => {
    try {
        const plans = await DefaultWorkoutPlan.find({});
        res.json({ success: true, data: plans });
    } catch (err) {
        console.error('Error fetching default plans:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

/**
 * @route GET /api/workout/recommendation
 * @desc Recommend a workout plan based on user questionnaire
 * @access Public
 */
router.get('/recommendation', async (req, res) => {
    const { userId } = req.query;

    if (!userId) {
        return res.status(400).json({ success: false, message: 'User ID is required' });
    }

    try {
        // Fetch user and their questionnaire 
        const user = await User.findById(userId);
        const { fitnessGoals, activityLevel } = user.questionnaire;

        // Determine level from activityLevel
        let level = 'Beginner';
        if (activityLevel.includes('Moderately')) level = 'Intermediate';
        if (activityLevel.includes('Very') || activityLevel.includes('Extra')) level = 'Advanced';

        // Match plan by goal and level
        const plan = await DefaultWorkoutPlan.findOne({
            focus: fitnessGoals,
            planName: new RegExp(level, 'i')
        });

        if (!plan) {
            return res.status(404).json({ success: false, message: 'No matching plan found' });
        }

        res.json({ success: true, data: plan });
    } catch (err) {
        console.error('Recommendation error:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});


// Export the router
module.exports = router;
