"""added unsafebehaviours relationship to location

Revision ID: a3f304f3cf61
Revises: 1753f920cf2f
Create Date: 2024-10-14 17:13:27.424235

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a3f304f3cf61'
down_revision: Union[str, None] = '1753f920cf2f'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
