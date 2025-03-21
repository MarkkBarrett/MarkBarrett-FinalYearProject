const mongoose = require('mongoose');

// Questionnaire schema
const QuestionnaireSchema = new mongoose.Schema({
    gender: { type: String, required: false },
    age: { type: Number, required: false },
    height: { type: Number, required: false },
    weight: { type: Number, required: false },
    fitnessGoals: { type: String, required: false },
    activityLevel: { type: String, required: false },
    dietaryInfo: { type: String, required: false }
});

// User schema
const UserSchema = new mongoose.Schema({
    firstName: { type: String, required: true },
    lastName: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    hasCompletedQuestionnaire: { type: Boolean, default: false },
    questionnaire: { type: QuestionnaireSchema, required: false }, // Embedded questionnaire data
    createdAt: { type: Date, default: Date.now }
});

// Export the User model
module.exports = mongoose.model('User', UserSchema);
