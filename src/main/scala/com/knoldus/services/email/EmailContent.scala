package com.knoldus.services.email

import com.knoldus.domain.recommendation.Recommendation
import com.knoldus.domain.session.SessionUnattendedUserInfo
import com.knoldus.domain.user.UserInformation
import com.knoldus.services.scheduler.SessionScheduler.EmailInfo
import com.knoldus.services.scheduler.UsersBanScheduler.EmailBodyInfo

object EmailContent {

  def setContentForFeedback(email: String, topic: String): String =
    s"""
       |Hi Knolder,
       |Thank you! for your valuable feedback
       |We'll let $email know your views on the Knolx Session's Topic $topic
       |Also you can change your response anytime until this feedback form is active.
       |
       |Thanks,
       |
       |Admin
       |
       |""".stripMargin

  def setContentForSessionReview(
    email: String,
    topic: String,
    date: String,
    category: String,
    subCategory: String
  ): String =
    s"""
       |Hi,
       |A session is requested by $email with following details
       |
       |Topic -> $topic
       |Date -> $date
       |Category -> $category
       |Sub-Category -> $subCategory
       |
       |Please review session details and take the required action.
       |""".stripMargin

  def setContentForPresenter(topic: String, date: String): String =
    s"""
       |Hi,
       |Your Knolx/Meetup has been scheduled with following details
       |
       |Topic: $topic
       |Date: $date
       |
       |It is recommended that the slide deck is prepared a week in advance and is shared with
       |the Knolx organizer as soon as it is prepared.This will help in getting important feedback
       |for any scope of improvements if required.
       |
       |
       |Please ensure the following for your session
       |
       |1. Knolx etiquette slide is added at the start of your slide deck.
       |2. Please make sure people understand its importance at the start of the session.
       |3. Screen recording app is installed on your laptop.
       |
       |For instructions "https://sites.google.com/a/knoldus.com/wiki/development/knolx-session-form"
       |
       |
       |Screen recording is started before starting your session.
       |Get in touch with the Knolx team for any assistance.
       |
       |Inform the Knolx organizer or the Knolx team if internet connectivity is required
       |during the session.
       |
       |It'd help in audio recording if the questions are repeated before answering them.
       |This is because collar MIC out is directly recorded.
       |
       |Don't forget to check out the feedback of your session (only visible to you and
       |captured anonymously) which will be available on Knolx Portal under
       |"knolx.knoldus.com/reports/user" once the feedback form is closed.
       |
       |For any queries you can get in touch with the Knolx organizer.
       |
       |Thanks,
       |
       |Admin
       |
       |""".stripMargin

  def setFeedbackMailContent(emailsInfo: List[EmailInfo], link: String): String =
    s"""
       |<p>Hi Knolders,</p>
       |<p>
       |    Hope you enjoyed the following Knolx session:
       |    <br>
       |    <strong>${emailsInfo.head.topic}</strong> by ${emailsInfo.head.presenter} held on ${emailsInfo.head.date}
       |    <br>
       |    Please submit your valuable feedback here: <a href="$link">Click Here</a>
       |    <br>
       |</p>
       |
       |<br>
       |<strong>Important Note</strong>
       |<ul>
       |    <li>It is very important for everyone to respond to the feedback form. If you have not attended the session, kindly fill "Did not attend" option with relevant reason.</li>
       |    <li>Your feedback is valuable, so think through and provide relevant feedback, as it is anonymous to the presenters. Do mention the comments if there are any improvement areas for the presenter.</li>
       |</ul>
       |
       |<p>Kindly submit your feedback before feedback form expires.</p>
       |
       |<p>
       |    Thanks,
       |    <br>
       |    Knolx Team
       |</p>
       |""".stripMargin

  def setReminderMailContent(link: String, emailsInfo: List[EmailInfo]): String = {
    val listOfSession =
      for (info <- emailsInfo)
        yield s"""
        <strong>${info.topic}</strong> by ${info.presenter} held on ${info.date}
      """

    s"""
       |<p>Hi Knolders,</p>
       |
       |<p>
       |    A gentle reminder to please share your feedback for the following session(s) latest by the
       |    <strong> end of the day</strong>.</p>
       |
       |<ul>
    ${listOfSession.mkString}
       |
       |<p>
       |    Your feedback is valuable for the presenter, <a href="$link">Click Here</a> to submit your feedback.
       |</p>
       |
       |<p>
       |    Thanks,
       |    <br>
       |    Admin
       |</p>
       |""".stripMargin
  }

  def setNotificationMailContent(emailsInfo: List[EmailInfo]): String = {
    val listOfSessions =
      for (info <- emailsInfo)
        yield s""" <li><strong>${info.topic}</strong> by ${info.presenter} held on ${info.date}
      </li>"""
    s"""
       |<p>Hi Knolders,</p>
       |<p>Knolx session(s) scheduled for today</p>
       |<ul>
           ${listOfSessions.mkString}
       |</ul>
       |<p><Strong>Timings</Strong></p>
       |<p>Each session is of 30 minutes followed by 10 minutes QA session, hence
       |    Please plan your work accordingly</p>
       |<p>
       |    Thanks,
       |    <br>
       |    Admin
       |</p>
       |""".stripMargin
  }

  def setContentForBanUser(emailsInfo: List[EmailBodyInfo]): String = {
    val listOfSessions =
      for (info <- emailsInfo) yield s"""<li>${info.topic} by ${info.presenter} held on ${info.date}</li>"""
    s""" <p>Hi,</p>
       |
       |    <p>
       |      You're banned from attending and presenting Knolx sessions for <strong>1 month</strong> as you did not submit
       |      feedback for the following session(s):
       |    </p>
       |    <ul>
         ${listOfSessions.mkString}
       |    </ul>
       |
       |      <p>
       |        Getting banned more than 3 times would result in a permanent ban.
       |        <br>
       |          If you think this is an error please contact Knolx Admin Team.
       |        </p>
       |
       |        <p>
       |          Thanks,
       |          <br>
       |            Admin
       |          </p>
       |""".stripMargin
  }

  def setUserUnbannedEmailContent(unbannedEmail: String): String =
    s"""<p>Hi Knolder ,</p>
       |<p>
       |    <strong>Congratulations</strong> !!
       |    <br>
       |    <br>
       |    The email id $unbannedEmail is unbanned from Knowledge Portal now and able to attend and present sessions.
       |    <br>
       |<strong>Don't forget to fill the feedback form.</strong>
       |</p>
       | <p>
       |          Thanks,
       |          <br>
       |            Admin
       |</p>""".stripMargin

  def setContentForMonthlyReportOfUsersNotAttendedSession(
    notAttendingUsersWithTopic: List[SessionUnattendedUserInfo]
  ): String = {
    val notAttendedListContent = notAttendingUsersWithTopic map {
          case SessionUnattendedUserInfo(session, emails) =>
            val emailBody = emails.map(email => s"<li>$email</li>").mkString("")
            s""" <br>Session:<strong>$session</strong> not attended by following users <br>&nbsp;$emailBody"""
        }
    s""" <p>Hi,</p>
       |
       |    <p>
       |      Monthly report of session not attended users
       |    </p>
       |    <ul>
       |    ${notAttendedListContent.mkString}
       |    </ul>
       |        <p>
       |          Thanks,
       |          <br>
       |            Admin
       |          </p>""".stripMargin

  }

  def setRecommendationEmailContent(recommendationInfo: Recommendation): String = {

    val recommendationEmail =
      if (recommendationInfo.email.isDefined)
        s""" <li>Email -> ${recommendationInfo.email.get}</li>"""

    s"""<p>Hi,</p>
       |<p>
       |    A recommendation has been given by <strong>${recommendationInfo.name}</strong> with following details
       |</p>
       |<ul>
           $recommendationEmail
       |    <li>Topic -> ${recommendationInfo.topic}</li>
       |    <li>Description -> ${recommendationInfo.description}</li>
       |    <li>Date -> ${recommendationInfo.submissionDate}</li>
       |</ul>
       |
       |<p>
       |Please review the recommendation details and take the required action: <a href="http://knolx.knoldus.com/recommendation">Click Here</a>
       |</p>""".stripMargin
  }

  def setContentForForgotPassword(link: String): String =
    s"""<p>Hi,</p>
       |<p>Please click <a href="$link">here</a> to reset your <strong>knolx portal</strong> password.</p></br></br>
       |<strong><p>If you are not the one who initiated this request kindly ignore this mail.</p></strong>
                """.stripMargin

  def setContentForBanUserReport(
    usersInfo: List[UserInformation],
    allBannedUsersOfLastMonth: List[UserInformation]
  ): String = {

    val listOfCurrentBannedUsers =
      for (info <- usersInfo)
        yield s"""<li>${info.email} is banned till ${info.banTill} and banned ${info.banCount} times</li>"""

    val bannedUsersOfLastMonth =
      for (user <- allBannedUsersOfLastMonth) yield s"""<li>${user.email} was banned on ${user.lastBannedOn.get}"""

    s"""<p>Hi Knolders,</p>
       |Total user(s) who was ban in last month
       |<ul>
       |${bannedUsersOfLastMonth.mkString}
       |</ul>
       |<p>Current Banned User(s)</p>
       |<ul>
       ${listOfCurrentBannedUsers.mkString}
       |</ul>
       |""".stripMargin
  }

  def setContentForActivateAccountEmail(link: String): String =
    s"""<p>Hi,</p>
       |Click <a href =$link>here</a> to activate your account""".stripMargin

}
