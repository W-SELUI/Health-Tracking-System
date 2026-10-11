# Integration Test Checklist

Coordinator: William Selui
Feature branch: william-provider-access
Latest verified feature commit: 7511d40

## Completed verification

- [x] Full automated suite: 155 tests, 0 failures, 0 errors.
- [x] Patient can grant, revoke and restore provider consent.
- [x] Authorized provider can view the patient's health history.
- [x] Provider can view weight and blood-pressure details.
- [x] Revoking consent blocks history and record details immediately.
- [x] Restoring consent allows access again.
- [x] Provider record pages contain no editing or deletion controls.
- [x] Automated tests cover unauthorized roles, other providers,
      changed patient/record IDs and disabled provider accounts.
- [x] Patient/provider messages and replies were manually verified.
- [x] Automated messaging tests verify participant privacy and validation.

## Final integrated checks — still pending

Run against the agreed merged version and record the result.

- [ ] Registration, login and profile updates — Jiya.
- [ ] Health records and analytics for selected periods — Yash.
- [ ] Activity creation, history and summaries — Shaniza.
- [ ] Medications, doses, adherence and appointments — Tasmiya.
- [ ] Consent, provider record access and messaging — William.
- [ ] Complete patient-to-provider flow after the final merge.
- [ ] Full automated suite passes on the merged revision.
- [ ] README explains setup, database configuration and test commands.
- [ ] Fresh checkout builds and starts using documented setup.
- [ ] Demo evidence and contribution records are ready.

## Integration questions to resolve

- Appointments currently store doctorName as text.
  Confirm alignment with the shared provider User identity required
  by the work distribution — Tasmiya and William.
- Analytics integration has not yet been verified in this branch
  snapshot — confirm with Yash.

## Final run record

Merged commit:
Date:
Tester:
Passed checks:
Failures and responsible owners:
Retest result: