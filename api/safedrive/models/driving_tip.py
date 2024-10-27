from uuid import uuid4
from sqlalchemy import Column, ForeignKey, String, Boolean, UUID, Date
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from safedrive.database.base import Base


def generate_uuid():
        return str(uuid4())  # Generates a UUID string

class DrivingTip(Base):
    """
    DrivingTip is the SQLAlchemy ORM model class representing the 'driving_tips' table.

    Attributes:
    - tip_id (UUID): Primary key representing the unique identifier for each driving tip.
    - title (str): The title of the driving tip.
    - meaning (str, optional): A supportive explanation of the behavior described by the tip.
    - penalty (str, optional): Information about applicable penalties.
    - fine (str, optional): Information about applicable fines.
    - law (str, optional): The specific law related to the unsafe driving behavior.
    - hostility (str, optional): Indicator for any hostility recorded in the behavior.
    - summary_tip (str, optional): Summary providing actionable advice.
    - sync (bool): Indicator whether the data has been synchronized with the server.
    - date (Date): The date on which the tip was recorded or generated.
    - profile_id (UUID): Foreign key reference to the driver profile associated with the tip.
    - llm (str, optional): The language model used to generate the tip.
    """
    
    __tablename__ = "driving_tips"

    tip_id = Column(String(36), primary_key=True, default=generate_uuid)
    title = Column(String(255), nullable=False)
    meaning = Column(String(255))
    penalty = Column(String(255))
    fine = Column(String(255))
    law = Column(String(255))
    hostility = Column(String(255))
    summary_tip = Column(String(255))
    sync = Column(Boolean, nullable=False)
    date = Column(Date, nullable=False)
    profile_id = Column(String(36), ForeignKey('driver_profile.driver_profile_id'), nullable=False)
    llm = Column(String(255))

    # Relationship with DriverProfile
    profile = relationship("DriverProfile", back_populates="driving_tips")
    
    def __repr__(self):
        return f"<DrivingTips(tip_id={self.tip_id}, title={self.title})>"

