import React, { useEffect, useRef, useState } from "react";
import AuthPage from "./components/AuthPage";
import ModuleList from "./components/ModuleList";
import RoadmapForm from "./components/RoadmapForm";
import {
  completeModule,
  generateRoadmap,
  getModules,
  getProgress,
  getRoadmaps,
  login,
  register
} from "./api";
import "./styles/app.css";

const emptyProgress = {
  totalModules: 0,
  completedModules: 0,
  completionPercent: 0
};

function App() {
  const [token, setToken] = useState(localStorage.getItem("sf_token"));
  const [page, setPage] = useState("home");
  const [roadmaps, setRoadmaps] = useState([]);
  const [activeRoadmap, setActiveRoadmap] = useState(null);
  const [modules, setModules] = useState([]);
  const [progress, setProgress] = useState(emptyProgress);
  const [generationNotice, setGenerationNotice] = useState("");
  const [appError, setAppError] = useState("");
  const [authError, setAuthError] = useState("");
  const [loading, setLoading] = useState(false);
  const roadmapIdRef = useRef(null);

  useEffect(() => {
    if (token) {
      restoreSession();
    }
  }, [token]);

  const extractError = (error) =>
    error?.response?.data?.error || "Something went wrong. Please try again.";

  const persistToken = (nextToken) => {
    localStorage.setItem("sf_token", nextToken);
    setToken(nextToken);
  };

  const clearSession = () => {
    localStorage.removeItem("sf_token");
    roadmapIdRef.current = null;
    setToken(null);
    setRoadmaps([]);
    setActiveRoadmap(null);
    setModules([]);
    setProgress(emptyProgress);
    setGenerationNotice("");
    setAppError("");
    setAuthError("");
    setPage("home");
  };

  const restoreSession = async () => {
    setLoading(true);
    setAppError("");
    setGenerationNotice("");
    try {
      const roadmapResponse = await getRoadmaps();
      const items = roadmapResponse.data;
      setRoadmaps(items);

      if (items.length > 0) {
        setActiveRoadmap(items[0]);
        const latestRoadmapId = items[0].id;
        roadmapIdRef.current = latestRoadmapId;
        const [moduleResponse, progressResponse] = await Promise.all([
          getModules(latestRoadmapId),
          getProgress(latestRoadmapId)
        ]);
        setModules(moduleResponse.data);
        setProgress(progressResponse.data);
        setPage("modules");
      } else {
        setActiveRoadmap(null);
        setModules([]);
        setProgress(emptyProgress);
        setPage("home");
      }
    } catch (error) {
      clearSession();
      setAuthError(extractError(error));
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (payload) => {
    setLoading(true);
    setAuthError("");
    try {
      const response = await register(payload);
      return response.data.token;
    } catch (error) {
      setAuthError(extractError(error));
      return null;
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (payload) => {
    setLoading(true);
    setAuthError("");
    try {
      const response = await login(payload);
      return response.data.token;
    } catch (error) {
      setAuthError(extractError(error));
      return null;
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async (payload) => {
    setLoading(true);
    setAppError("");
    try {
      const response = await generateRoadmap(payload);
      const roadmap = response.data;
      roadmapIdRef.current = roadmap.roadmapId;
      setActiveRoadmap({
        id: roadmap.roadmapId,
        goal: roadmap.goal,
        level: roadmap.level,
        preferences: roadmap.preferences,
        completed: false
      });
      setModules(roadmap.modules || []);
      setGenerationNotice(roadmap.generationMessage || "");
      setProgress({
        totalModules: (roadmap.modules || []).length,
        completedModules: 0,
        completionPercent: 0
      });
      const roadmapsResponse = await getRoadmaps();
      setRoadmaps(roadmapsResponse.data);
      setPage("modules");
    } catch (error) {
      setAppError(extractError(error));
      setGenerationNotice("");
    } finally {
      setLoading(false);
    }
  };

  const handleComplete = async (moduleId) => {
    setLoading(true);
    setAppError("");
    try {
      const updatedModuleResponse = await completeModule(moduleId);
      setModules((current) =>
        current.map((module) =>
          module.id === moduleId ? updatedModuleResponse.data : module
        )
      );

      if (roadmapIdRef.current) {
        const progressResponse = await getProgress(roadmapIdRef.current);
        setProgress(progressResponse.data);
        setRoadmaps((current) =>
          current.map((roadmap) =>
            roadmap.id === roadmapIdRef.current
              ? {
                  ...roadmap,
                  completed:
                    progressResponse.data.totalModules > 0 &&
                    progressResponse.data.totalModules === progressResponse.data.completedModules
                }
              : roadmap
          )
        );
      }
    } catch (error) {
      setAppError(extractError(error));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <main className="app-shell">
        <AuthPage
          error={authError}
          loading={loading}
          onAuthSuccess={persistToken}
          onLogin={handleLogin}
          onRegister={handleRegister}
        />
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark" />
          <span className="brand-text">SkillForge</span>
        </div>
        <div className="topbar-actions">
          <p className="topbar-tagline">
            {page === "modules"
              ? "Track progress and open your curated resources."
              : "Forge your path. Master your skills."}
          </p>
          <button className="sf-button sf-button-secondary" type="button" onClick={() => setPage("home")}>
            + New Roadmap
          </button>
          <button className="sf-button sf-button-ghost" type="button" onClick={clearSession}>
            Sign Out
          </button>
        </div>
      </header>

      {page === "home" ? (
        <RoadmapForm
          error={appError}
          loading={loading}
          notice={generationNotice}
          onGenerate={handleGenerate}
        />
      ) : (
        <ModuleList
          loading={loading}
          modules={modules}
          onComplete={handleComplete}
          progress={progress}
          roadmap={activeRoadmap}
        />
      )}

      {roadmaps.length > 0 ? (
        <section className="sf-card sf-card-history">
          <div className="section-heading">
            <span className="sf-badge">Recent Roadmaps</span>
            <h2>Jump back into a saved path</h2>
          </div>
          <div className="history-list">
            {roadmaps.map((roadmap) => (
              <button
                key={roadmap.id}
                className="history-item"
                type="button"
                onClick={async () => {
                  setLoading(true);
                  setAppError("");
                  try {
                    roadmapIdRef.current = roadmap.id;
                    setActiveRoadmap(roadmap);
                    const [moduleResponse, progressResponse] = await Promise.all([
                      getModules(roadmap.id),
                      getProgress(roadmap.id)
                    ]);
                    setModules(moduleResponse.data);
                    setProgress(progressResponse.data);
                    setPage("modules");
                  } catch (error) {
                    setAppError(extractError(error));
                  } finally {
                    setLoading(false);
                  }
                }}
              >
                <strong>{roadmap.goal}</strong>
                <span>{roadmap.level}</span>
                <small>{roadmap.completed ? "Completed" : "Active"}</small>
              </button>
            ))}
          </div>
        </section>
      ) : null}

      {appError && page === "modules" ? (
        <div className="sf-error sf-error-floating">{appError}</div>
      ) : null}

      {generationNotice && page === "modules" ? (
        <div className="sf-notice sf-notice-floating">{generationNotice}</div>
      ) : null}
    </main>
  );
}

export default App;
