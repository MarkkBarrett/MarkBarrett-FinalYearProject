const express = require('express');
const bcrypt = require('bcryptjs'); // For hashing passwords
const User = require('../models/User'); // Import the User schema

// Create a router to handle API endpoints
const router = express.Router();

/**
 * @route POST /api/auth/register
 * @desc Register a new user
 * @access Public
 */
router.post('/register', async (req, res) => {
    const { firstName, lastName, email, password } = req.body;

    try {
        // Check if the user already exists in the database
        let user = await User.findOne({ email });
        if (user) {
            return res.status(400).json({ msg: 'User already exists' }); // Return error if user exists
        }

        // Create a new user object
        user = new User({
            firstName,
            lastName,
            email,
            password: await bcrypt.hash(password, 10), // Hash the password before saving
        });

        await user.save(); // Save the user to the database
        res.json({ msg: 'User registered successfully' }); // Send success response
    } catch (err) {
        console.error(err.message); // Log error for debugging
        res.status(500).send('Server error'); // Return server error response
    }
});


/**
 * @route POST /api/auth/login
 * @desc Login user and determine redirection
 * @access Public
 */
router.post('/login', async (req, res) => {
    const { email, password } = req.body;

    try {
        // Check email is valid
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(404).json({ msg: 'User not found' });
        }

        //Check passwords match
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(400).json({ msg: 'Invalid credentials' });
        }

        const redirectTo = user.hasCompletedQuestionnaire ? 'Dashboard' : 'Questionnaire'; //Handle intents
        res.json({
            msg: 'Login successful',
            user: {
                _id: user._id.toString(), // Ensure _id is returned as a string
                firstName: user.firstName,
                lastName: user.lastName,
                email: user.email,
                hasCompletedQuestionnaire: user.hasCompletedQuestionnaire,
            },
            redirectTo
        });
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server error');
    }
});

/**
 * @route POST /api/auth/questionnaire
 * @desc Save questionnaire data for a user
 * @access Public
 */
router.post('/questionnaire', async (req, res) => {
    const { email, questionnaire } = req.body;

    try {
        // get user and update
        const user = await User.findOneAndUpdate(
            { email },
            { $set: { questionnaire, hasCompletedQuestionnaire: true } }, // set questionnaire and boolean
            { new: true } // return updated user version
        );

        if (!user) {
            return res.status(404).json({ msg: 'User not found' });
        }

        res.json({ msg: 'Questionnaire saved successfully', user });
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server error');
    }
});

// Export the router for use in server.js
module.exports = router;
