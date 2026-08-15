import React, { useState } from "react";

const initialState = {
  goal: "",
  level: "Beginner",
  preferences: "Hands-on"
};

function RoadmapForm({ onGenerate, loading, error, notice }) {
  const [form, setForm] = useState(initialState);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    await onGenerate(form);
  };

  return (
    <section className="generator-layout">
      <div className="generator-copy">
        <span className="sf-badge">AI-Powered Learning</span>
        <h2 className="hero-title">
          Build your
          <span> learning</span>
          <span> roadmap</span>
          <span> in seconds.</span>
        </h2>
        <p className="hero-description">
          Tell us your goal, your level, and how you learn best. We&apos;ll
          generate a structured path with curated resources, free and paid.
        </p>
      </div>

      <section className="generator-card">
        <div className="auth-copy">
          <h2>Create Your Roadmap</h2>
          <p>Fill in the details below to get started</p>
        </div>

        <form className="sf-form" onSubmit={handleSubmit}>
          <label>
            <span>What do you want to learn?</span>
            <input
              name="goal"
              value={form.goal}
              onChange={handleChange}
              placeholder="DATA SCIENCE"
              required
            />
          </label>

          <div className="form-grid-two">
            <label>
              <span>Your level</span>
              <div className="select-shell">
                <select name="level" value={form.level} onChange={handleChange}>
                  <option>Beginner</option>
                  <option>Intermediate</option>
                  <option>Advanced</option>
                </select>
                <span className="select-chevron">v</span>
              </div>
            </label>

            <label>
              <span>Learning style</span>
              <div className="select-shell">
                <select
                  name="preferences"
                  value={form.preferences}
                  onChange={handleChange}
                  required
                >
                  <option>Hands-on</option>
                  <option>Project-based</option>
                  <option>Video-first</option>
                  <option>Reading-focused</option>
                  <option>Structured bootcamp</option>
                </select>
                <span className="select-chevron">v</span>
              </div>
            </label>
          </div>

          <div className="generator-hint">
            SkillForge will tailor the roadmap around your goal, experience, and
            preferred way of learning.
          </div>

          {loading ? (
            <div className="sf-notice">
              Generating roadmap. If one API key hits its limit, SkillForge will
              automatically try the next key and then switch to failsafe only if
              all keys are exhausted.
            </div>
          ) : null}

          {notice ? <div className="sf-notice">{notice}</div> : null}

          {error ? <div className="sf-error">{error}</div> : null}

          <button className="sf-button" type="submit" disabled={loading}>
            <span>{loading ? "Generating..." : "Generate Roadmap"}</span>
            <span className="button-arrow">{"->"}</span>
          </button>
        </form>
      </section>
    </section>
  );
}

export default RoadmapForm;
