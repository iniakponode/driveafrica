from pydantic import BaseModel, Field, EmailStr
from typing import Optional
from uuid import UUID

class DriverProfileBase(BaseModel):
    """
    Base schema for Driver Profile entity. Defines common attributes.

    Attributes:
    - **driver_profile_id**: Unique identifier for the driver profile.
    - **email**: Email address of the driver.
    - **sync**: Indicates if the driver profile has been synced.
    """
    driver_profile_id: UUID = Field(
        ...,
        description="Unique identifier for the driver profile."
    )
    email: EmailStr = Field(
        ...,
        description="Email address of the driver."
    )
    sync: Optional[bool] = Field(
        False,
        description="Indicates if the driver profile has been synced."
    )

    class Config:
        from_attributes = True  # Pydantic v2

class DriverProfileCreate(BaseModel):
    """
    Schema for creating a new Driver Profile.

    Attributes:
    - **email**: Email address of the driver.
    - **sync**: Optional sync status.
    """
    email: EmailStr = Field(
        ...,
        description="Email address of the driver."
    )
    sync: Optional[bool] = Field(
        False,
        description="Sync status of the driver profile."
    )

    class Config:
        from_attributes = True

class DriverProfileUpdate(BaseModel):
    """
    Schema for updating an existing Driver Profile.

    Attributes:
    - **email**: Optional updated email address.
    - **sync**: Optional updated sync status.
    """
    email: Optional[EmailStr] = Field(
        None,
        description="Updated email address of the driver."
    )
    sync: Optional[bool] = Field(
        None,
        description="Updated sync status."
    )

    class Config:
        from_attributes = True

class DriverProfileResponse(DriverProfileBase):
    """
    Schema for representing a Driver Profile response.
    """
    pass
