const express = require('express');
const mongoose = require('mongoose');
const WorkoutSession = require('../models/WorkoutSession'); // Workout session schema
const WorkoutPlan = require('../models/WorkoutPlan'); // Workout plan schema
const router = express.Router();

/**
 * @route POST /api/workout/session
 * @desc Save a completed workout session
 * @access Public
 */

router.post('/session', async (req, res) => {
    
    let { _id, sessionDate, workoutName, exerciseLogs } = req.body;

    const userId = _id; // Directly map _id to userId

    if (!userId || !sessionDate || !workoutName || !exerciseLogs) {
        return res.status(400).json({ success: false, message: 'Missing required fields' });
    }

    try {
        // Convert userId to ObjectId (might not need this)
        const objectId = new mongoose.Types.ObjectId(userId);

        // Create new workout session
        const newSession = new WorkoutSession({
            userId: objectId,
            sessionDate,
            workoutName,
            exerciseLogs
        });

        await newSession.save();

        // Move to the next workout in the user's workout plan
        const userPlan = await WorkoutPlan.findOne({ userId: objectId });

        if (userPlan) {
            // Move to the next workout (loop back if at the end)
            const nextDayIndex = (userPlan.currentDayIndex + 1) % userPlan.workouts.length;
            userPlan.currentDayIndex = nextDayIndex;
            await userPlan.save();
        }

        // return messages
        res.json({ success: true, message: 'Workout session saved', nextDayIndex: userPlan ? userPlan.currentDayIndex : 0 });
    } catch (err) {
        console.error('Error saving workout session:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

module.exports = router;
