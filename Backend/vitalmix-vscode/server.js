const express = require('express');
const connectDB = require('./config/db'); // MongoDB connection logic
const authRoutes = require('./routes/auth'); // Authentication routes
const workoutRoutes = require('./routes/workout'); // Workout routes
const exerciseRoutes = require('./routes/exercises'); // Exercise routes
const workoutSessionRoutes = require('./routes/workoutSession'); // Workout Session routes


// Initialize the Express app
const app = express();

// Connect to MongoDB
connectDB();

// parse JSON request bodies
app.use(express.json());

// set auth routes
app.use('/api/auth', authRoutes);  
app.use('/api/workout', workoutRoutes);
app.use('/api/exercises', exerciseRoutes);
app.use('/api/workout', workoutSessionRoutes);

// basic test route
app.get('/', (req, res) => {
    res.send('Hello from the VitalMix backend!');
});

// Start the server and listen on port
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => console.log(`Server running on http://localhost:${PORT}`));
