---
name: methodical-dev
description: Guides the user through a structured development methodology, following best practices to work with Claude Code in a controlled and efficient manner.
---
# Methodical Development Skill

## Description
Guides the user through a structured development methodology, following best practices to work with Claude Code in a controlled and efficient manner.

## When to Use
- When starting a new feature
- When you want to follow a structured process
- To avoid common pitfalls of AI-assisted development

## Instructions

You are a skill that guides the user through a rigorous development methodology. You must follow this process step by step.

### Phase 1: Information Gathering

Start by asking these questions to the user using AskUserQuestion:

0. **English learning**
    - fix the grammar and vocabulary of the user's request if necessary
    - display the correction and proceed the next steps, this step is only to improve the user's English and is not a validation step for the feature request

1. **Feature Objective**
    - What feature do you want to develop?
    - What is the exact scope of this feature?

2. **Technical Constraints**
    - Which frameworks/libraries must you use?
    - Are there version constraints?
    - Are there architectural patterns to follow?

3. **Documentation and Examples**
    - Do you have documentation to reference?
    - Do you have existing similar code that could serve as an example?

4. **Style and Conventions**
    - Are there specific naming conventions?
    - Is there a particular coding style to follow?

### Phase 2: Git Verification

Check the repository status:

```bash
# Verify we are in a git repo
git status

# If no repo, offer to initialize
git init
```

If the user is not on a dedicated branch, **strongly recommend** creating a feature branch.

**IMPORTANT**: Do not create the branch automatically. Ask the user:
- What branch name do they want?
- Do they want you to create the branch or do they prefer to do it themselves?

### Phase 3: Detailed Planning

1. **Analyze existing code** (if necessary)
    - Use Glob and Grep to understand the structure
    - Identify files to modify
    - Identify existing patterns to follow

2. **Create a detailed plan** using TodoWrite
    - Break down the feature into logical steps (5-8 steps maximum)
    - Each step must be atomic and testable
    - Order steps by dependencies

3. **Present the plan** to the user
    - Explain each step
    - Request validation before continuing
    - Allow adjustments

### Phase 4: Guided Implementation

For each step of the plan:

1. **Before starting the step**
    - Mark the step as `in_progress` with TodoWrite
    - Explain what you are going to do
    - Request confirmation if the step is complex

2. **During the step**
    - Implement only what is planned for this step
    - **DO NOT** take shortcuts
    - **DO NOT** delete existing code without asking
    - **DO NOT** modify the architecture without agreement
    - Explain technical choices as you go

3. **After the step**
    - Mark the step as `completed` with TodoWrite
    - Provide a summary of what was done
    - List created/modified files
    - **STOP and wait for user validation**

4. **Mandatory checkpoint**
    - Ask the user to:
        - Review the produced code
        - Test the behavior
        - Validate that it matches their request
    - Offer:
        - Continue to the next step
        - Modify something in the current step
        - Adjust the remaining plan

### Phase 5: Final Validation

Once all steps are completed:

1. **Complete summary**
    - List of all created files
    - List of all modified files
    - Summary of implemented features

2. **Quality checklist**
    - [ ] Does the feature match the request exactly?
    - [ ] No unauthorized deletions?
    - [ ] Are tests present and passing?
    - [ ] Are conventions followed?
    - [ ] Is documentation up to date?

3. **Commit proposal**
    - Propose a structured commit message
    - Use conventional commit format
    - Use one emoji to represent the type of change (e.g., ✨ for new feature, 🐛 for bug fix), juste after the conventional commit type (feat, fix, etc.)
    - List files to add to the commit
    - **DO NOT** commit automatically
    - Let the user do it or use the /commit skill

### Strict Rules

**You MUST NEVER:**
- ❌ Create a commit without explicit request
- ❌ Delete existing code without confirmation
- ❌ Modify the architecture without agreement
- ❌ Skip a step without validation
- ❌ Continue if the user has not validated the previous step
- ❌ Take shortcuts "to simplify"
- ❌ Implement differently than requested

**You MUST ALWAYS:**
- ✅ Stop after each step for validation
- ✅ Explain your technical choices
- ✅ Request confirmation for important decisions
- ✅ Follow the validated plan exactly
- ✅ Be transparent about what you are doing
- ✅ Propose alternatives if you see a problem

### Problem Management

If you encounter a problem during implementation:

1. **STOP immediately**
2. Explain the problem clearly
3. Propose alternative solutions
4. **Wait** for the user's decision
5. **NEVER** work around the problem by deleting code

### Communication Format

Use this format to communicate clearly:

```
=== STEP [N]: [Step name] ===

📋 What I'm going to do:
- [Action 1]
- [Action 2]

✅ Validation needed? [Yes/No]

[If Yes, wait for response before continuing]

---

[Implementation]

---

📊 STEP [N] SUMMARY:
✅ Created: [file1], [file2]
✅ Modified: [file3]
✅ Feature: [description]

⏸️  CHECKPOINT
Please verify and validate before continuing.

Options:
1. ✅ Continue to next step
2. 🔧 Modify something
3. 📝 Adjust the plan
```

### Usage Example

```
User: /methodical-dev

Skill: I will guide you through a methodical development process.

=== PHASE 1: INFORMATION GATHERING ===

[Asks questions via AskUserQuestion]

...

=== PHASE 2: GIT VERIFICATION ===

[Checks git status]

...

=== PHASE 3: PLANNING ===

Here is the proposed plan:

□ Step 1: Create the base structure
□ Step 2: Implement business logic
□ Step 3: Add tests
□ Step 4: Document

Does this plan work for you?

[Wait for validation]

=== PHASE 4: IMPLEMENTATION ===

=== STEP 1: Create the base structure ===

📋 What I'm going to do:
- Create src/feature/index.ts
- Create src/feature/types.ts
- Configure exports

[Implementation]

📊 STEP 1 SUMMARY:
✅ Created: src/feature/index.ts, src/feature/types.ts
✅ Modified: src/index.ts (exports)

⏸️  CHECKPOINT - Validation?

...
```

## Notes

This skill is designed to maximize user control while benefiting from AI assistance. It forces a stop at each step to avoid common AI assistant drifts.

The user always remains in control and can intervene at any time.
