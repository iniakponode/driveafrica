from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class NLGReportBase(BaseModel):
    """
    Base class for the NLG Report model, defining common attributes.

    Attributes:
    - id (int): The unique identifier for the NLG report.
    - userId (str): The user identifier related to the report.
    - reportText (str): The text of the NLG report.
    - dateRange (str): The date range for which the report is applicable.
    - synced (bool): Indicator whether the report has been synced.
    """
    id: int
    userId: str
    reportText: str
    dateRange: str
    synced: bool

    class Config:
        orm_mode = True

class NLGReportCreate(BaseModel):
    """
    Schema for creating a new NLG Report record.

    Attributes:
    - userId (str): The user identifier related to the report.
    - reportText (str): The text of the NLG report.
    - dateRange (str): The date range for which the report is applicable.
    - synced (Optional[bool]): Indicator whether the report has been synced, defaults to False.
    """
    userId: str
    reportText: str
    dateRange: str
    synced: Optional[bool] = False

    class Config:
        orm_mode = True

class NLGReportUpdate(BaseModel):
    """
    Schema for updating an existing NLG Report record.

    Attributes:
    - reportText (Optional[str]): Optionally update the text of the report.
    - dateRange (Optional[str]): Optionally update the date range of the report.
    - synced (Optional[bool]): Optionally update the sync status.
    """
    reportText: Optional[str] = None
    dateRange: Optional[str] = None
    synced: Optional[bool] = None

    class Config:
        orm_mode = True

class NLGReportResponse(NLGReportBase):
    """
    Schema for the response format of an NLG Report record.

    Inherits all attributes from NLGReportBase.
    """
    pass