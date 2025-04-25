const express = require('express');
const mongoose = require('mongoose'); //remove and test... unnused 
const WorkoutPlan = require('../models/WorkoutPlan');
const DefaultWorkoutPlan = require('../models/DefaultWorkoutPlan');
const WorkoutSession = require('../models/WorkoutSession');
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

/**
 * @route POST /api/workout/choosePlan
 * @desc Assign a chosen default workout plan to a user
 * @access Public
 */

router.post('/choosePlan', async (req, res) => {
    const { userId, chosenPlanId } = req.body;

    // Validate inputs
    if (!userId || !chosenPlanId) {
        return res.status(400).json({ success: false, message: 'userId and chosenPlanId are required' });
    }

    try {
        // Remove existing workout plan for this user, if any
        await WorkoutPlan.deleteOne({ userId });

        // Find the default plan the user selected
        const defaultPlan = await DefaultWorkoutPlan.findById(chosenPlanId);

        // Check if the selected default plan exists
        if (!defaultPlan) {
            return res.status(404).json({ success: false, message: 'Selected workout plan not found' });
        }

        // Clone default plan into a user WorkoutPlan
        const newPlan = new WorkoutPlan({
            userId,
            planName: defaultPlan.planName,
            description: defaultPlan.description,
            focus: defaultPlan.focus,
            workouts: defaultPlan.workouts,
            currentDayIndex: 0 // Always starts from day 0
        });

        // Save new plan
        await newPlan.save();

        // Send back success response
        res.json({ success: true, message: 'Workout plan assigned successfully', data: newPlan });
    } catch (err) {
        console.error('Error assigning workout plan:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

/**
 * @route GET /api/workout/lastSessionByName
 * @desc Fetch the latest workout session by workout name for a user
 * @access Public
 */
router.get('/lastSessionByName', async (req, res) => {
    const { userId, workoutName } = req.query;

    // Validate input
    if (!userId || !workoutName) {
        return res.status(400).json({ success: false, message: 'Missing userId or workoutName' });
    }

    try {
        // Find latest session with the same workout name
        const lastSession = await WorkoutSession.findOne({ userId, workoutName })
            .sort({ sessionDate: -1 }); // Sort by date descending to get the latest

        if (!lastSession) {
            return res.status(404).json({ success: false, message: 'No matching session found' });
        }

        res.json({ success: true, data: lastSession });
    } catch (err) {
        console.error('Error fetching last session', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

/**
 * @route GET /api/workout/history
 * @desc Fetch all workout sessions for a user
 * @access Public
 */
router.get('/history', async (req, res) => {
    const { userId } = req.query;

    // Validate input
    if (!userId) {
        return res.status(400).json({ success: false, message: 'Missing userId' });
    }

    try {
        // Find all workout sessions for the user, sorted by most recent first
        const sessions = await WorkoutSession.find({ userId }).sort({ sessionDate: -1 });

        if (!sessions || sessions.length === 0) {
            return res.status(404).json({ success: false, message: 'No workout sessions found' });
        }

        // Return all sessions
        res.json({ success: true, data: sessions });
    } catch (err) {
        console.error('Error fetching workout history', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

// Export the router
module.exports = router;
