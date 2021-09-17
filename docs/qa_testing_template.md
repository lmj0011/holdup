Quality Assurance Testing Template
=====================

QA testing is done by testing functionality of each of the app's Fragments.
Since testing is currently done manually, test cases should be brief and focus on core functionality operating as expected under normal conditions.

### pre-QA testing tasks:
- [ ] Update dependencies
- [ ] check TODOs
- [ ] fix all warnings and errors produced by the linter

On a fresh release build app install, test the following:

## HomeFragment

- [ ] create a submission of each type
- [ ] toggle display options
- [ ] toggle between Light/Dark mode (check for visual inconsistencies)

## SubmissionFragment

- [ ] create a submission of each type
    - [ ] "Post Now" for each submission type
    - [ ] schedule for each submission type
- [ ] toggle between Light/Dark mode (check for visual inconsistencies)
- [ ] create 10 scheduled submission at least 1 hour apart (**can be done as last test**) 
    - [ ] verify all publish on time (+/- 5 minutes)

## EditSubmissionFragment

- [ ] change submission attributes and save
- [ ] reschedule submission
- [ ] "Post Now" submission
- [ ] delete submission

## Feedback Dialog

- [ ] check that the email templates are correct.
    - [ ] "bug report" should omit any personal info or secret credentials 
    - [ ] "general feedback" should be blank

## About Dialog

- [ ] check that version info is correct
