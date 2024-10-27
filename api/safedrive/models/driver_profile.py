from uuid import uuid4
from sqlalchemy import Column, String, Boolean
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from safedrive.database.base import Base

def generate_uuid():
        return str(uuid4())  # Generates a UUID string
    
class DriverProfile(Base):
    
    __tablename__ = "driver_profile"

    driver_profile_id = Column(String(36), primary_key=True, default=generate_uuid)
    email = Column(String(50), unique=True, nullable=False)
    sync = Column(Boolean, nullable=False)

    # Relationship with DrivingTips
    driving_tips = relationship("DrivingTip", back_populates="profile")
    trip_data = relationship("Trip", back_populates="driver_profile")
    
    def __repr__(self):
        return f"<DriverProfile(driver_profile_id={self.driver_profile_id}, email={self.email})>"