const express = require('express');
const router = express.Router();
const User = require('../models/User'); // Import the User model

/**
 * @route GET /api/profile
 * @desc Get user profile information by userId
 * @access Public
 */

router.get('/', async (req, res) => {
    const { userId } = req.query; // Get userId from query parameter

    // Validate input
    if (!userId) {
        return res.status(400).json({ success: false, message: 'User ID is required' });
    }

    try {
        // Find the user by their MongoDB _id
        const user = await User.findById(userId);

        // If user not found, return 404
        if (!user) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }

        // Structure the profile response
        const profileData = {
            name: `${user.firstName} ${user.lastName}`,
            email: user.email,
            age: user.questionnaire?.age || null,
            height: user.questionnaire?.height || null,
            weight: user.questionnaire?.weight || null
        };

        // Return success and the profile data
        res.status(200).json({ success: true, data: profileData });

    } catch (err) {
        console.error('Error fetching profile:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

module.exports = router;
