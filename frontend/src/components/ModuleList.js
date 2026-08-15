import React from "react";

function parseResource(resource) {
  if (!resource) {
    return { label: "Resource", url: "", valid: false };
  }

  const [labelPart, urlPart] = resource.split("|");
  const label = (labelPart || "Resource").trim();
  const url = (urlPart || resource).trim();
  const valid = /^https?:\/\//i.test(url);
  return { label, url, valid };
}

function ModuleList({ modules, progress, onComplete, loading, roadmap }) {
  return (
    <section className="roadmap-layout">
      <div className="roadmap-head">
        <div className="roadmap-head-copy">
          <h2>Your Learning Roadmap</h2>
          <div className="roadmap-tags">
            <span>{roadmap?.level || "Level"}</span>
            <span>{roadmap?.preferences || "Learning Style"}</span>
            <span>{roadmap?.goal || "Goal"}</span>
          </div>
        </div>
        <div className="roadmap-progress">
          <span className="progress-label">Progress</span>
          <div className="progress-meta">
            <strong>
              {progress.completedModules} / {progress.totalModules}
            </strong>
            <span>{progress.completionPercent}%</span>
          </div>
          <div className="progress-bar">
            <span style={{ width: `${progress.completionPercent}%` }} />
          </div>
        </div>
      </div>

      <div className="module-grid">
        {modules.map((module) => {
          const primaryResource = parseResource(module.freeResource);
          const secondaryResource = parseResource(module.paidResource);

          return (
            <article
              key={module.id}
              className={`module-card ${module.completed ? "is-complete" : ""}`}
            >
              <div className="module-header">
                <span className="module-index">
                  MODULE {String(module.moduleOrder).padStart(2, "0")}
                </span>
                <span className="module-status">
                  {module.completed ? "Done" : "In Progress"}
                </span>
              </div>
              <h3>{module.title}</h3>
              <p>{module.description}</p>

              <div className="resource-box">
                <strong>Free</strong>
                {primaryResource.valid ? (
                  <a
                    className="resource-link"
                    href={primaryResource.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {primaryResource.label}
                  </a>
                ) : (
                  <span className="resource-link resource-link-disabled">
                    {primaryResource.label}
                  </span>
                )}
              </div>

              <div className="resource-box resource-box-paid">
                <strong>Paid</strong>
                {secondaryResource.valid ? (
                  <a
                    className="resource-link"
                    href={secondaryResource.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {secondaryResource.label}
                  </a>
                ) : (
                  <span className="resource-link resource-link-disabled">
                    {secondaryResource.label}
                  </span>
                )}
              </div>

              <button
                className="sf-button sf-button-secondary"
                type="button"
                disabled={module.completed || loading}
                onClick={() => onComplete(module.id)}
              >
                {module.completed ? "Completed" : "Mark Complete"}
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export default ModuleList;
