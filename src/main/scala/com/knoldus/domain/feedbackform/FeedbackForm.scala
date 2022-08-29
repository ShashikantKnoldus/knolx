package com.knoldus.domain.feedbackform

final case class FeedbackForm(name: String, questions: List[Question], active: Boolean)
