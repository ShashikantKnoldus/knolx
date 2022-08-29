package com.knoldus.bootstrap

import com.knoldus.dao.clientsecrets.ClientSecretsDao
import com.knoldus.dao.feedbackform.{ FeedbackFormDao, FeedbackFormsResponseDao }
import com.knoldus.dao.holiday.HolidayDao
import com.knoldus.dao.recommendation.{ RecommendationDao, RecommendationsResponseDao }
import com.knoldus.dao.session.{ CalenderDao, CategoryDao, NewSessionDao, SessionDao }
import com.knoldus.dao.tag.{ SessionTagMappingDao, TagDao }
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.user.UserDao
import org.mongodb.scala.MongoDatabase

class DaoInstantiator(database: MongoDatabase) {

  lazy val feedbackFormDao = new FeedbackFormDao(database)
  lazy val recommendationDao = new RecommendationDao(database)
  lazy val recommendationsResponseDao = new RecommendationsResponseDao(database)
  lazy val feedbackFormResponseDao = new FeedbackFormsResponseDao(database)
  lazy val sessionDao = new SessionDao(database)
  lazy val newSessionDao = new NewSessionDao(database)
  lazy val userDao = new UserDao(database)
  lazy val categoryDao = new CategoryDao(database)
  lazy val calendarDao = new CalenderDao(database)
  lazy val clientDao = new ClientSecretsDao(database)
  lazy val tagDao = new TagDao(database)
  lazy val slotDao = new SlotDao(database)
  lazy val sessionTagMappingDao = new SessionTagMappingDao(database)
  lazy val holidayDao = new HolidayDao(database)

}
