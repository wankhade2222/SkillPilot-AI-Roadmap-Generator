import React, { useState } from "react";

const defaultForm = {
  name: "",
  email: "",
  password: ""
};

function AuthPage({ onAuthSuccess, onLogin, onRegister, error, loading }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(defaultForm);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const action = mode === "login" ? onLogin : onRegister;
    const payload =
      mode === "login"
        ? { email: form.email, password: form.password }
        : form;

    const token = await action(payload);
    if (token) {
      setForm(defaultForm);
      onAuthSuccess(token);
    }
  };

  return (
    <div className="auth-shell">
      <section className="hero-copy">
        <div className="brand-lockup">
          <span className="brand-mark" />
          <span className="brand-text">SkillForge</span>
        </div>

        <div className="hero-copy-inner">
          <h1 className="hero-title">
            Your skills,
            <span> forged</span>
            <span> here.</span>
          </h1>
          <p className="hero-description">
            AI-powered learning roadmaps tailored to your goal, your level, and
            the way you learn best.
          </p>
          <div className="feature-stack">
            <div className="feature-pill">
              <strong>Fast</strong>
              <span>Generated in seconds</span>
            </div>
            <div className="feature-pill">
              <strong>Goal</strong>
              <span>Tailored to your goal</span>
            </div>
            <div className="feature-pill">
              <strong>Learn</strong>
              <span>Free and paid resources</span>
            </div>
            <div className="feature-pill">
              <strong>Track</strong>
              <span>Track your progress</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-card">
        <div className="tab-switcher">
          <button
            className={mode === "login" ? "active" : ""}
            type="button"
            onClick={() => setMode("login")}
          >
            Sign In
          </button>
          <button
            className={mode === "register" ? "active" : ""}
            type="button"
            onClick={() => setMode("register")}
          >
            Create Account
          </button>
        </div>

        <div className="auth-copy">
          <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
          <p>
            {mode === "login"
              ? "Sign in to continue your learning journey"
              : "Start building AI-powered learning plans in seconds"}
          </p>
        </div>

        <form className="sf-form" onSubmit={handleSubmit}>
          {mode === "register" && (
            <label>
              <span>Name</span>
              <input
                name="name"
                value={form.name}
                onChange={handleChange}
                placeholder="Your name"
                required
              />
            </label>
          )}

          <label>
            <span>Email</span>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="you@example.com"
              required
            />
          </label>

          <label>
            <span>Password</span>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="********"
              required
            />
          </label>

          {error ? <div className="sf-error">{error}</div> : null}

          <button className="sf-button" type="submit" disabled={loading}>
            <span>
              {loading ? "Working..." : mode === "login" ? "Sign In" : "Create Account"}
            </span>
            <span className="button-arrow">{"->"}</span>
          </button>
        </form>

        <p className="auth-footer">
          {mode === "login" ? "Don't have an account?" : "Already have an account?"}{" "}
          <button
            className="auth-footer-link"
            type="button"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "Create one free" : "Sign in"}
          </button>
        </p>
      </section>
    </div>
  );
}

export default AuthPage;
