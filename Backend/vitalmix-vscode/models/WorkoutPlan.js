const mongoose = require('mongoose');

// Workout schema (each day in the plan)
const WorkoutSchema = new mongoose.Schema({
    workoutName: { type: String, required: true },
    exercises: [{ type: String, required: true }] // Exercise IDs
});

// Main WorkoutPlan schema
const WorkoutPlanSchema = new mongoose.Schema({
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, // Link to User
    planName: { type: String, required: true },
    description: { type: String },
    focus: [{ type: String, required: true }], // build muscle etc.
    currentDayIndex: { type: Number, default: 0 }, // Tracks progress
    workouts: [WorkoutSchema] // Array of workouts
});

// Export the WorkoutPlan model
module.exports = mongoose.model('WorkoutPlan', WorkoutPlanSchema);
