-- Backfill topic column for episode 71 (2026-03-25)
-- Topic: Claude Code auto mode & code review
UPDATE episode_articles SET topic = 'Claude Code auto mode and code review' WHERE id IN (
    2314, -- Auto mode for Claude Code
    2320, -- Posts from @BBleimschein (code-review-graph author)
    2343, -- Code Review Agent Benchmark
    2334  -- Dynamic analysis enhances issue resolution
);

-- Topic: Agentic coding agents at scale
UPDATE episode_articles SET topic = 'Agentic coding at scale' WHERE id IN (
    2339, -- Immersion in the GitHub Universe: Scaling Coding Agents to Mastery
    2344, -- The Evolution of Tool Use in LLM Agents
    2327, -- Self-Evolving Skill Engine with OpenSpace
    2331, -- Rethinking the Role of Entropy in Optimizing Tool-Use Behaviors
    2318, -- Posts from @Vtrivedy10
    2321  -- Posts from @AlphaSignalAI
);

-- Topic: RL post-training and model efficiency
UPDATE episode_articles SET topic = 'RL post-training and model efficiency' WHERE id IN (
    2326, -- NVIDIA AI Introduces PivotRL
    2325, -- Google Introduces TurboQuant
    2324  -- Paged Attention in Large Language Models
);

-- Topic: MCP security risks
UPDATE episode_articles SET topic = 'MCP security risks' WHERE id IN (
    2329, -- Model Context Protocol Threat Modeling and Tool Poisoning
    2316  -- Posts from @amasad
);

-- Topic: Code quality and secure code generation
UPDATE episode_articles SET topic = 'Code quality and secure generation' WHERE id IN (
    2333, -- Does Teaming-Up LLMs Improve Secure Code Generation?
    2342, -- AI-Generated Code Is Not Reproducible (Yet)
    2332, -- ConceptCoder: Improve Code Reasoning via Concept Learning
    2330, -- From Context to Intent: Reasoning-Guided Function-Level Code Completion
    2315  -- From Brittle to Robust: Improving LLM Annotations for SE Optimization
);

-- Topic: Algorithm synthesis and formal methods
UPDATE episode_articles SET topic = 'Algorithm synthesis' WHERE id IN (
    2340  -- Early Discoveries of Algorithmist I
);

-- Topic: Software engineering with AI
UPDATE episode_articles SET topic = 'AI in software engineering' WHERE id IN (
    2341, -- ReqFusion: A Multi-Provider Framework for Automated PEGS Analysis
    2336, -- LLM-Powered Workflow Optimization for Multidisciplinary Software Development
    2335  -- From Technical Debt to Cognitive and Intent Debt
);

-- Topic: Industry updates
UPDATE episode_articles SET topic = 'Industry updates' WHERE id IN (
    2317, -- Posts from @felixrieseberg
    2323, -- Posts from @AnthropicAI
    2322, -- Posts from @sama
    2319  -- Posts from @Vtrivedy10
);


-- Backfill topic column for episode 69 (2026-03-24)
-- Topic: Anthropic Claude computer use and Dispatch
UPDATE episode_articles SET topic = 'Claude computer use and Dispatch' WHERE id IN (
    2306, -- RT: releasing feature that allows Claude to control your computer
    2296, -- Posts from @AnthropicAI
    2295  -- Performance improvements for the CLI + Dispatch
);

-- Topic: Meta Hyperagents
UPDATE episode_articles SET topic = 'Meta Hyperagents' WHERE id IN (
    2299  -- Meta AI's New Hyperagents
);

-- Topic: MCP security and tool auditing
UPDATE episode_articles SET topic = 'MCP security and tool auditing' WHERE id IN (
    2303, -- Semantic Tool Discovery for LLMs: Vector-Based MCP Tool Selection
    2301, -- Auditing MCP Servers for Over-Privileged Tool Capabilities
    2302  -- Are AI-assisted Development Tools Immune to Prompt Injection?
);

-- Topic: Claude Code productivity
UPDATE episode_articles SET topic = 'Claude Code productivity' WHERE id IN (
    2308, -- How I'm Productive with Claude Code
    2305, -- Posts from @felixrieseberg
    2310  -- Posts from @badlogicgames
);

-- Topic: Agent tooling and knowledge sharing
UPDATE episode_articles SET topic = 'Agent tooling and knowledge sharing' WHERE id IN (
    2309, -- Show HN: Cq – Stack Overflow for AI coding agents
    2300, -- Production-Ready AI Agent with Colab-MCP
    2304, -- Posts from @noahzweben
    2307, -- Posts from @caspar_br
    2313  -- Posts from @jarredsumner
);

-- Topic: AI research (vision/world models)
UPDATE episode_articles SET topic = 'AI research' WHERE id IN (
    2298, -- Luma Labs Launches Uni-1
    2297  -- Yann LeCun's LeWorldModel
);
