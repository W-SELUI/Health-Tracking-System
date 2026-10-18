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

## Negative-input and misuse checks

Run these checks with the local application and record each result as Pass or
Fail. A passing check means the invalid action is rejected, a useful message
is shown, and no invalid data or permission is saved.

### Messaging

- [ ] MSG-N01 — Submit an empty message. Expected: `Message cannot be empty.`
- [ ] MSG-N02 — Submit a message containing only spaces. Expected: rejected
      and not saved.
- [ ] MSG-N03 — Submit more than 4,000 characters. Expected: `Message must
      be at most 4,000 characters.`
- [ ] MSG-N04 — As a provider, start a new conversation with a patient who
      has not granted active consent. Expected: access denied.
- [ ] MSG-N05 — Try to open a conversation with your own user ID or an
      unknown user ID. Expected: access denied.
- [ ] MSG-N06 — As an administrator, open `/messages`. Expected: access
      denied because messaging is for patients and providers.

### Provider access and consent

- [ ] PA-N01 — Submit a second request while the first request is still
      pending. Expected: duplicate request rejected; only one pending request.
- [ ] PA-N02 — Request access again after the patient has approved the first
      request. Expected: rejected because access is already active.
- [ ] PA-N03 — Open a provider or record URL with an invalid patient ID such
      as `999999`. Expected: not-found or access-denied response.
- [ ] PA-N04 — Try to approve another patient's request ID. Expected: request
      rejected and its status unchanged.
- [ ] PA-N05 — Submit approval, rejection or cancellation again after the
      request has already been completed. Expected: only pending requests can
      be changed.
- [ ] PA-N06 — Try the legacy direct-grant action, if still visible. Expected:
      direct granting is rejected; the provider must request access first.

### Health records and proposals

- [ ] REC-N01 — Submit a health record without selecting a metric. Expected:
      `Health metric is required.`
- [ ] REC-N02 — Submit a scalar record without a measurement value. Expected:
      `Measurement value is required.`
- [ ] REC-N03 — Submit without an observation date/time. Expected: observation
      date and time validation message.
- [ ] REC-N04 — Enter a future observation date. Expected: `Observation time
      cannot be in the future.`
- [ ] REC-N05 — Enter more than 1,000 characters in Notes. Expected: notes
      length validation message and no record/proposal saved.
- [ ] REC-N06 — Submit blood pressure with only systolic or only diastolic.
      Expected: the missing-value validation message.
- [ ] REC-N07 — Try a negative measurement such as `-20`. Expected: reject
      medically invalid values; if accepted, record it as a validation gap.
- [ ] REC-N08 — Open a record or proposal URL with ID `999999`. Expected:
      not-found/error response and no data exposed.
- [ ] CR-N01 — As a provider, submit a proposal with a missing value or future
      observation date. Expected: validation error and no pending proposal.
- [ ] CR-N02 — Approve or reject a nonexistent proposal ID. Expected:
      not-found/error response and the permanent record remains unchanged.
- [ ] CR-N03 — Approve the same proposal twice. Expected: second action is
      rejected and the record is not changed twice.
- [ ] CR-N04 — Revoke provider consent while a proposal is pending, then try
      to approve it. Expected: approval is blocked because provider access is
      no longer active.

### Security and role protection

- [ ] SEC-N01 — As a patient, open `/provider/patients`. Expected: access
      denied.
- [ ] SEC-N02 — As a provider, open `/patient/consents`. Expected: access
      denied.
- [ ] SEC-N03 — As a provider, open a patient's patient-edit/delete URL.
      Expected: access denied; permanent records are not modified.
- [ ] SEC-N04 — Revoke consent, then use the provider's old record link.
      Expected: access denied and the patient's records remain hidden.
- [ ] SEC-N05 — Log out and open a protected page directly. Expected:
      redirect to login with no protected data displayed.
