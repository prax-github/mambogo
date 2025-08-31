# Cursor Rules - Professional Software Engineering Standards

This directory contains comprehensive Cursor rules based on lessons learned from the SEC-08 implementation journey and professional software engineering best practices.

## 📋 Rule Files Overview

### Core Rules
- **`delivery-rules.mdc`** - Core delivery and verification standards (existing)
- **`project-context-rules.mdc`** - Project documentation and context requirements (CRITICAL)
- **`communication-rules.mdc`** - Professional communication and user interaction protocols
- **`completion-rules.mdc`** - Task completion verification and deliverable requirements (NEW)

### Technical Implementation Rules
- **`spring-boot-rules.mdc`** - Spring Boot specific patterns and bean configuration
- **`code-quality-rules.mdc`** - Code quality standards and development practices
- **`workflow-rules.mdc`** - Development workflow and process management (UPDATED)
- **`testing-rules.mdc`** - Comprehensive testing strategies and standards

### Java-Specific Rules
- **`java-coding-standards.mdc`** - Java coding standards, naming conventions, and style guidelines
- **`java-best-practices.mdc`** - Advanced Java development patterns and object-oriented principles
- **`java-modern-features.mdc`** - Modern Java features (Java 8+) and functional programming
- **`java-performance.mdc`** - Performance optimization and efficiency best practices
- **`java-anti-patterns.mdc`** - Common mistakes, code smells, and anti-patterns to avoid

### Architecture & Security Rules
- **`microservices-rules.mdc`** - Distributed systems and microservices best practices
- **`security-rules.mdc`** - Security implementation and compliance standards

## 🎯 **Java Rules Coverage Matrix**

| Category | Rule File | Key Areas Covered |
|----------|-----------|-------------------|
| **Standards** | java-coding-standards.mdc | Formatting, naming, class design, method structure |
| **Best Practices** | java-best-practices.mdc | OOP principles, design patterns, error handling |
| **Modern Java** | java-modern-features.mdc | Lambdas, streams, Optional, records, pattern matching |
| **Performance** | java-performance.mdc | Memory management, collections, I/O, concurrency |
| **Anti-Patterns** | java-anti-patterns.mdc | Code smells, common mistakes, what to avoid |

## 🎯 Purpose

These rules prevent the types of failures experienced in SEC-08:
- **Premature completion claims** without verification
- **Spring Bean configuration conflicts** and dependency issues
- **Code quality problems** and technical debt accumulation
- **Testing failures** and integration issues
- **Communication gaps** and expectation management

## 📚 **CRITICAL: Project Context Requirements**

The `project-context-rules.mdc` enforces **MANDATORY** documentation review before any task:

### **Required Reading Before Any Implementation:**
1. **Product Requirements Document (PRD).md** - Business requirements and specifications
2. **E-commerce Microservices MVP — Execution Roadmap.md** - Project execution plan and milestones  
3. **implementation log/*.md** - All previous implementation logs for context and patterns
4. **interview notes/*.md** - Technical context and architectural decisions
5. **docs/RSL-01-IMPLEMENTATION-SUMMARY.md** - Overall project implementation summary

### **Context Analysis Protocol:**
- **Business Context**: Understand problem being solved and acceptance criteria
- **Technical Context**: Review existing services, patterns, and architectures
- **Implementation History**: Learn from previous implementations and avoid repeated mistakes
- **Integration Points**: Identify dependencies and integration requirements

### **Benefits of Context-Aware Development:**
- **Consistency**: Follow established patterns and naming conventions
- **Efficiency**: Reuse existing components and configurations
- **Quality**: Apply lessons learned from previous implementations
- **Integration**: Ensure compatibility with existing systems
- **Alignment**: Maintain coherence with business requirements and project roadmap

## 🔄 Rule Application

- **`alwaysApply: true`** - Core rules applied to all relevant files
- **File-specific globs** - Targeted rules for specific file types
- **Context-aware enforcement** - Rules adapt to the development context

## 🚀 Key Benefits

1. **Failure Prevention**: Systematic prevention of common implementation failures
2. **Quality Assurance**: Automated enforcement of professional standards
3. **Process Consistency**: Standardized workflows across all implementations
4. **Communication Excellence**: Professional interaction patterns
5. **Security Compliance**: Enterprise-grade security implementation standards

## 📚 Integration with Main .cursorrules

These modular rules complement the main `.cursorrules` file by providing:
- **Granular control** for specific domains (Spring Boot, testing, security)
- **File-type specific guidance** for targeted rule application
- **Organized rule management** for easier maintenance and updates

## 🎓 Learning Integration

Based on the SEC-08 retrospective, these rules embody:
- **Professional Paranoia** - Always verify everything
- **Systematic Verification** - Structured quality gates
- **Honest Communication** - Transparent progress reporting
- **Technical Excellence** - Enterprise-grade implementation standards

## 🔧 Usage

Cursor automatically applies these rules based on:
- File type and location (via globs)
- Development context and patterns
- Professional standards enforcement
- Quality gate requirements

## 🎯 Critical New Addition: Completion Rules

### completion-rules.mdc (Added based on SEC-09 lessons learned)

**Purpose**: Prevents premature completion claims and ensures all deliverables are complete

**Key Features:**
- **MANDATORY COMPLETION CHECKLIST** - Required verification before claiming task complete
- **Standard Deliverables Verification** - Ensures implementation log + interview guide creation
- **Context Switching Protection** - Prevents abandoning current work due to new feedback
- **TODO List Enforcement** - All TODO items must be completed before task closure

**Trigger Conditions:**
- Before updating roadmap status
- Before claiming any task as "complete"
- When receiving mid-task feedback or direction changes

**Example Usage:**
```
BEFORE marking SEC-X as complete:
✅ All TODO items marked "completed"
✅ Implementation log created
✅ Interview guide created
✅ All linting warnings resolved
✅ Integration testing completed
✅ Zero compilation errors
```

This rule system ensures that future implementations maintain the high professional standards learned from the SEC-08 and SEC-09 experiences.
