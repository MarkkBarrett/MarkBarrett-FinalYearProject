const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs'); // For hashing passwords
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

/**
 * @route PUT /api/profile/change-password
 * @desc Change user's password
 * @access Public 
 */
router.put('/changePassword', async (req, res) => {
    let { _id, currentPassword, newPassword } = req.body;

    const userId = _id; // Directly map _id to userId

    // Validate input
    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ success: false, message: 'All fields are required' });
    }

    // Prevent using the same password again
    if (currentPassword === newPassword) {
        return res.status(400).json({ success: false, message: 'New password must be different from current password' });
}

    try {
        // Find the user
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }

        // Compare existing password
        const isMatch = await bcrypt.compare(currentPassword, user.password);
        if (!isMatch) {
            return res.status(401).json({ success: false, message: 'Current password is incorrect' });
        }

        // Hash and update new password (CHECK THIS AGAIN)
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(newPassword, salt);
        user.password = hashedPassword;
        await user.save();

        res.status(200).json({ success: true, message: 'Password updated successfully' });

    } catch (err) {
        console.error('Error changing password:', err.message);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

module.exports = router;
