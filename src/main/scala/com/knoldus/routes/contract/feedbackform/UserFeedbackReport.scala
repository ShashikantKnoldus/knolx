package com.knoldus.routes.contract.feedbackform

final case class UserFeedbackReport(email: String, coreMember: Boolean, questionResponse: List[QuestionResponse])
