package net.hedtech.restfulapi

class Parent {
    String name

    static hasMany = [children: Child]


    static constraints = {
    }
}
