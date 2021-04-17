package com.rminhas.parsing

import com.rminhas.generated.proto.adressbook.{AddressBook, Person}

import java.io.{FileInputStream, FileNotFoundException, FileOutputStream}
import scala.io.StdIn
import scala.util.Using

object ParsingExampleApp extends App {

  val FILE_NAME = "addressbook.pb"
  def readFromFile(): AddressBook =
    Using(new FileInputStream(FILE_NAME)) { fileInputStream =>
      AddressBook.parseFrom(fileInputStream)
    }.recover {
      case _: FileNotFoundException =>
        println("No address book found. Will create a new file.")
        AddressBook()
    }.get

  def personFromStdin(): Person = {
    print("Enter person ID (int): ")
    val id = StdIn.readInt()
    print("Enter name: ")
    val name = StdIn.readLine()
    print("Enter email address (blank for none): ")
    val email = StdIn.readLine()

    def getPhone(): Option[Person.PhoneNumber] = {
      print("Enter a phone number (or leave blank to finish): ")
      val number = StdIn.readLine()
      if (number.nonEmpty) {
        print("Is this a mobile, home, or work phone [mobile, home, work] ? ")
        val typ = StdIn.readLine() match {
          case "mobile" => Some(Person.PhoneType.MOBILE)
          case "home" => Some(Person.PhoneType.HOME)
          case "work" => Some(Person.PhoneType.WORK)
          case _ =>
            println("Unknown phone type. Leaving as None.")
            None
        }
        Some(Person.PhoneNumber(number = number, `type` = typ))
      } else None
    }

    // Keep prompting for phone numbers until None is returned.
    val phones =
      Iterator
        .continually(getPhone())
        .takeWhile(_.nonEmpty)
        .flatten
        .toSeq

    Person(
      id = id,
      name = name,
      email = if (email.nonEmpty) Some(email) else None,
      phones = phones
    )
  }

  def addPerson(): Unit = {
    val newPerson = personFromStdin()
    val addressBook = readFromFile()
    // Append the new person to the people list field
    val updated = addressBook.update(
      _.people :+= newPerson
    )
    Using(new FileOutputStream(FILE_NAME)) { output =>
      updated.writeTo(output)
    }
  }

  addPerson()
  println(readFromFile())
}
