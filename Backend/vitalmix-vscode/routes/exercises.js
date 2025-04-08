const express = require('express');
const Exercise = require('../models/Exercise'); // Import Exercise model

const router = express.Router();

/**
 * @route GET /api/exercises/byIds
 * @desc Fetch multiple exercises by their IDs
 * @access Public
 */
router.get('/byIds', async (req, res) => {
    const { exerciseIds } = req.query; // Get exercise IDs from query params

    if (!exerciseIds) {
        return res.status(400).json({ success: false, message: 'Exercise IDs are required' });
    }

    try {
        // Fix comma seperation to an array 
        const idsArray = Array.isArray(exerciseIds) ? exerciseIds : exerciseIds.split(',');

        // Fetch exercises matching provided IDs
        const exercises = await Exercise.find({ _id: { $in: idsArray } });

        if (!exercises.length) {
            return res.status(404).json({ success: false, message: 'No exercises found' });
        }

        res.json({ success: true, data: exercises }); // Return found exercises
    } catch (err) {
        console.error('Error fetching exercises:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

// Export router
module.exports = router;
