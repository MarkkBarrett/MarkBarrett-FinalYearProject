const mongoose = require('mongoose');

// Workout schema
const WorkoutSchema = new mongoose.Schema({
    workoutName: { type: String, required: true },
    exercises: [{ type: String, required: true }] // Exercise IDs
});

// DefaultWorkoutPlan schema for template plans
const DefaultWorkoutPlanSchema = new mongoose.Schema({
    _id: { type: String, required: true },
    planName: { type: String, required: true },
    description: { type: String },
    focus: [{ type: String, required: true }], // build muscle etc.
    workouts: [WorkoutSchema] // Array of workouts
});

// Export the DefaultWorkoutPlan model
module.exports = mongoose.model('DefaultWorkoutPlan', DefaultWorkoutPlanSchema, 'defaultWorkoutPlans');
