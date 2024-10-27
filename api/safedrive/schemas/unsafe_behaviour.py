from pydantic import BaseModel
from typing import Optional
from uuid import UUID
from datetime import datetime

class UnsafeBehaviourBase(BaseModel):
    """
    Base schema for Unsafe Behaviour data, containing common attributes shared across different schemas.

    Attributes:
    - id (UUID): Unique identifier for the unsafe behaviour.
    - tripId (UUID): Identifier of the trip associated with this unsafe behaviour.
    - locationId (Optional[UUID]): Identifier of the location associated with this behaviour.
    - behaviorType (str): Type of unsafe behaviour observed (e.g., speeding, harsh braking).
    - severity (float): Severity level of the unsafe behaviour.
    - timestamp (int): Epoch timestamp when the behaviour was recorded.
    - date (Optional[datetime]): Date when the behaviour occurred.
    - updatedAt (Optional[datetime]): Timestamp when the record was last updated.
    - updated (bool): Indicator of whether the record has been updated.
    - synced (bool): Indicator whether the data has been synced.
    - alcoholInfluence (bool): Indicator if alcohol influence was detected.
    """
    id: UUID
    tripId: UUID
    locationId: Optional[UUID] = None
    behaviorType: str
    severity: float
    timestamp: int
    date: Optional[datetime] = None
    updatedAt: Optional[datetime] = None
    updated: bool = False
    synced: bool = False
    alcoholInfluence: bool = False

    class Config:
        orm_mode = True

class UnsafeBehaviourCreate(BaseModel):
    """
    Schema for creating a new Unsafe Behaviour record.

    Attributes:
    - tripId (UUID): Identifier of the trip associated with this unsafe behaviour.
    - locationId (Optional[UUID]): Identifier of the location associated with this behaviour.
    - behaviorType (str): Type of unsafe behaviour observed (e.g., speeding, harsh braking).
    - severity (float): Severity level of the unsafe behaviour.
    - timestamp (int): Epoch timestamp when the behaviour was recorded.
    - date (Optional[datetime]): Date when the behaviour occurred.
    - alcoholInfluence (bool): Indicator if alcohol influence was detected.
    """
    tripId: UUID
    locationId: Optional[UUID] = None
    behaviorType: str
    severity: float
    timestamp: int
    date: Optional[datetime] = None
    alcoholInfluence: bool = False

    class Config:
        orm_mode = True

class UnsafeBehaviourUpdate(BaseModel):
    """
    Schema for updating an existing Unsafe Behaviour record.

    Attributes:
    - locationId (Optional[UUID]): Optionally update the location associated with this behaviour.
    - behaviorType (Optional[str]): Optionally update the type of unsafe behaviour.
    - severity (Optional[float]): Optionally update the severity level of the unsafe behaviour.
    - timestamp (Optional[int]): Optionally update the epoch timestamp when the behaviour was recorded.
    - date (Optional[datetime]): Optionally update the date when the behaviour occurred.
    - updatedAt (Optional[datetime]): Optionally update the timestamp for last update.
    - updated (Optional[bool]): Optionally update the update indicator.
    - synced (Optional[bool]): Optionally update the sync status.
    - alcoholInfluence (Optional[bool]): Optionally update the alcohol influence indicator.
    """
    locationId: Optional[UUID] = None
    behaviorType: Optional[str] = None
    severity: Optional[float] = None
    timestamp: Optional[int] = None
    date: Optional[datetime] = None
    updatedAt: Optional[datetime] = None
    updated: Optional[bool] = None
    synced: Optional[bool] = None
    alcoholInfluence: Optional[bool] = None

    class Config:
        orm_mode = True

class UnsafeBehaviourResponse(UnsafeBehaviourBase):
    """
    Schema for the response format of Unsafe Behaviour data.

    Inherits from UnsafeBehaviourBase.
    """
    pass