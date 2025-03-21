// Import mongoose for interacting with MongoDB
const mongoose = require('mongoose');

// Function to connect to MongoDB
const connectDB = async () => {
    try {
        // Connect using the MongoDB URI
        await mongoose.connect('mongodb://localhost:27017/VitalMix');

        console.log('MongoDB connected successfully'); // Log success message
    } catch (err) {
        console.error('MongoDB connection error:', err.message); // Log error message
        process.exit(1); // Exit process if connection fails
    }
};

// Export the connection function so it can be used in server.js
module.exports = connectDB;
