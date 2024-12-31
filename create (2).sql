CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointment (
    PName varchar(255),
    CName varchar(255),
    VName varchar(255),
    Time date,
    ApptID INT,
    PRIMARY KEY (ApptID),
    FOREIGN KEY (Pname) REFERENCES Patients(Username),
    FOREIGN KEY (Vname) REFERENCES Vaccines(Name),
    FOREIGN KEY (CName) REFERENCES Caregivers(Username)
);