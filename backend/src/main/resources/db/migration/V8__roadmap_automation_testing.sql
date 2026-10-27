-- Curated roadmap for the Automation Testing Engineer career path (path 12).
-- Skill ids: 18 Manual Testing, 7 SQL Basics, 1 Java Basics, 2 Core Java and OOP, 19 Selenium, 20 API Testing.
INSERT INTO learning_roadmaps (id, career_path_id, title, description)
VALUES (5, 12, 'Automation Testing Engineer roadmap', 'From manual testing fundamentals to Selenium and API test automation.');

INSERT INTO learning_roadmap_items (id, roadmap_id, title, description, step_order, estimated_duration) VALUES
  (33, 5, 'Testing fundamentals', 'Test cases, scenarios, bug life cycle, SDLC and STLC.', 1, '2 weeks'),
  (34, 5, 'SQL for testers', 'Query and verify data with SELECT, joins and aggregation.', 2, '2 weeks'),
  (35, 5, 'Java basics for automation', 'Syntax, strings, arrays, control flow and collections.', 3, '3 weeks'),
  (36, 5, 'Object-oriented Java', 'Classes, inheritance, interfaces and exception handling.', 4, '2 weeks'),
  (37, 5, 'Selenium WebDriver', 'Locators, waits, handling elements and Page Object Model.', 5, '4 weeks'),
  (38, 5, 'API testing', 'Postman, status codes, validation and authentication.', 6, '2 weeks'),
  (39, 5, 'Framework and portfolio project', 'TestNG, reports and a small end-to-end framework on GitHub.', 7, '4 weeks');

INSERT INTO learning_roadmap_item_skills (item_id, skill_id) VALUES
  (33, 18), (34, 7), (35, 1), (36, 2), (37, 19), (38, 20), (39, 19);
